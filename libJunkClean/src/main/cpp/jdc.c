#include <jni.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/stat.h>
#include <dirent.h>
#include <NdkHelper.h>
#include <string.h>
#include "src/7zTypes.h"

int extract7zBytes(Byte *decryptData, int decryptDataLength, Byte *un7zData, const char* storagePath, char *tempName);

long long get_file_size(char *base_file_path);

uint8_t key[] = {0x02, 0xcc, 0xc4, 0x16, 0xa7, 0xb4, 0x55, 0x77, 0x47, 0x0a, 0x92, 0xee, 0xd9, 0x6b, 0x00, 0x41};
uint8_t iv[] = {0xa9, 0xc1, 0x35, 0xd7, 0x99, 0x31, 0x64, 0x88, 0x6f, 0x48, 0xca, 0x95, 0x82, 0x1a, 0x4a, 0x02};

JNIEXPORT jobject JNICALL
Java_com_ihs_device_clean_junk_cache_app_nonsys_junk_task_ajs_a( //proguard scanAppJunkCache
        JNIEnv *env,
        jobject thiz,
        jbyteArray ajEncryptData, jbyteArray adEncryptData, jstring jstoragePath) {

    jclass scanUtilClass = (*env)->FindClass(env, "com/ihs/device/clean/junk/util/SUtils");
    jmethodID decryptMethodId = (*env)->GetStaticMethodID(env, scanUtilClass, "a", "([B[B[B)[B");
    jmethodID getInstalledPkgId = (*env)->GetStaticMethodID(env, scanUtilClass, "c", "()Ljava/util/HashSet;");

    jbyteArray jKey = (*env)->NewByteArray(env, 16);
    jbyteArray jVI = (*env)->NewByteArray(env, 16);

    (*env)->SetByteArrayRegion(env, jKey, 0, 16, (const jbyte *) key);
    (*env)->SetByteArrayRegion(env, jVI, 0, 16, (const jbyte *) iv);

    jobject ajDecryptData = (*env)->CallStaticObjectMethod(env, scanUtilClass, decryptMethodId, ajEncryptData, jKey, jVI);
    jobject adDecryptData = (*env)->CallStaticObjectMethod(env, scanUtilClass, decryptMethodId, adEncryptData, jKey, jVI);
    jobject pkgInstalledSet = (*env)->CallStaticObjectMethod(env, scanUtilClass, getInstalledPkgId);

    if (jstoragePath == NULL) {
        LOGE("jstoragePath == NULL");
        return NULL;
    }
    const char *storage_path = (*env)->GetStringUTFChars(env, jstoragePath, 0);

    (*env)->DeleteLocalRef(env, scanUtilClass);
    (*env)->DeleteLocalRef(env, jKey);
    (*env)->DeleteLocalRef(env, jVI);

    if (ajDecryptData == NULL || adDecryptData == NULL) {
        LOGE("ajDecryptData == NULL || adDecryptData == NULL");
        return NULL;
    }

    Byte *aj_decrypt_data = (Byte *) (*env)->GetByteArrayElements(env, ajDecryptData, 0);
    int aj_decrypt_data_length = (int) (*env)->GetArrayLength(env, ajDecryptData);

    Byte *ad_decrypt_data = (Byte *) (*env)->GetByteArrayElements(env, adDecryptData, 0);
    int ad_decrypt_data_length = (int) (*env)->GetArrayLength(env, adDecryptData);

    Byte *aj_un7z_data = malloc(1024 * 1024 * 2);
    Byte *ad_un7z_data = malloc(1024 * 1024 * 2);

    int aj_un7z_res = extract7zBytes(aj_decrypt_data, aj_decrypt_data_length, aj_un7z_data, storage_path, "aj");
    if (aj_un7z_res != 0) {
        return NULL;
    }

    int ad_un7z_res = extract7zBytes(ad_decrypt_data, ad_decrypt_data_length, ad_un7z_data, storage_path, "ad");
    if (ad_un7z_res != 0) {
        return NULL;
    }

    jclass appJunkClass = (*env)->FindClass(env, "com/ihs/device/clean/junk/cache/app/nonsys/junk/HSAppJunkCache");
    jmethodID appJunkConstruct = (*env)->GetMethodID(env, appJunkClass, "<init>", "(Ljava/lang/String;Ljava/lang/String;JLjava/lang/String;Ljava/lang/String;)V");
    jmethodID setInstalledId = (*env)->GetMethodID(env, appJunkClass, "setInstalled", "(Z)V");

    jclass arrayListClass = (*env)->FindClass(env, "java/util/ArrayList");
    jmethodID arrayListConstruct = (*env)->GetMethodID(env, arrayListClass, "<init>", "()V");
    jobject appJunkList = (*env)->NewObject(env, arrayListClass, arrayListConstruct, "");
    jmethodID addMethodId = (*env)->GetMethodID(env, arrayListClass, "add", "(Ljava/lang/Object;)Z");

    jclass hashSetClass = (*env)->FindClass(env, "java/util/HashSet");
    jmethodID hashSetConStruct = (*env)->GetMethodID(env, hashSetClass, "<init>", "()V");
    jmethodID setContainId = (*env)->GetMethodID(env, hashSetClass, "contains", "(Ljava/lang/Object;)Z");
    jmethodID setAddId = (*env)->GetMethodID(env, hashSetClass, "add", "(Ljava/lang/Object;)Z");
    jobject canntDeletePaths = (*env)->NewObject(env, hashSetClass, hashSetConStruct, "");

    jclass processorClass = (*env)->FindClass(env, "com/ihs/device/clean/junk/cache/app/nonsys/junk/task/ajs");
    jmethodID postProgressMethodId = (*env)->GetMethodID(env, processorClass, "postOnProgressUpdated", "([Ljava/lang/Object;)V");

    jclass progressClass = (*env)->FindClass(env, "com/ihs/device/clean/junk/cache/app/nonsys/junk/task/ajsp");
    jmethodID progressConstruct = (*env)->GetMethodID(env, progressClass, "<init>", "(ILcom/ihs/device/clean/junk/cache/app/nonsys/junk/HSAppJunkCache;)V");

    int path_count = 0;

    const char *split1 = "\n";
    const char *split2 = "\t";

    char *aj_outer_ptr = NULL;
    char *aj_inner_ptr = NULL;

    char *aj_line = strtok_r((char *) aj_un7z_data, split1, &aj_outer_ptr);
    if (aj_line == NULL) {
        return NULL;
    }
    aj_line = strtok_r(NULL, split1, &aj_outer_ptr);

    while (aj_line != NULL) {
        char *pkg = strtok_r(aj_line, split2, &aj_inner_ptr);
        if (pkg == NULL) {
            return NULL;
        }
        char *app_name = strtok_r(NULL, split2, &aj_inner_ptr);
        if (app_name == NULL) {
            return NULL;
        }
        char *type = strtok_r(NULL, split2, &aj_inner_ptr);
        if (type == NULL) {
            return NULL;
        }
        char *path = strtok_r(NULL, split2, &aj_inner_ptr);
        if (path == NULL) {
            return NULL;
        }
        char compPath[512] = "";
        strcat(compPath, storage_path);
        strcat(compPath, path);

        if (access(compPath, F_OK) == 0) {
            path_count++;

            jstring jpkg = (*env)->NewStringUTF(env, pkg);
            jstring jappName = (*env)->NewStringUTF(env, app_name);
            jstring jtype = (*env)->NewStringUTF(env, type);
            jstring jpath = (*env)->NewStringUTF(env, compPath);

            long long file_size = get_file_size(compPath);

            jobject appJunk = (*env)->NewObject(env, appJunkClass, appJunkConstruct, jpkg, jappName, file_size, jpath, jtype);

            jboolean isInstalled = (*env)->CallBooleanMethod(env, pkgInstalledSet, setContainId, jpkg);

            (*env)->CallVoidMethod(env, appJunk, setInstalledId, isInstalled);

            (*env)->CallBooleanMethod(env, appJunkList, addMethodId, appJunk);

            jobject objProgress = (*env)->NewObject(env, progressClass, progressConstruct, path_count, appJunk);

            jobjectArray objProgressArray = (*env)->NewObjectArray(env, 1, progressClass, objProgress);

            (*env)->CallVoidMethod(env, thiz, postProgressMethodId, objProgressArray);

            (*env)->DeleteLocalRef(env, jpkg);
            (*env)->DeleteLocalRef(env, jappName);
            (*env)->DeleteLocalRef(env, jtype);
            (*env)->DeleteLocalRef(env, jpath);
            (*env)->DeleteLocalRef(env, appJunk);
            (*env)->DeleteLocalRef(env, objProgress);
            (*env)->DeleteLocalRef(env, objProgressArray);
        }

        aj_line = strtok_r(NULL, split1, &aj_outer_ptr);
    }

    char *ad_outer_ptr = NULL;
    char *ad_inner_ptr = NULL;

    char *ad_line = strtok_r((char *) ad_un7z_data, split1, &ad_outer_ptr);
    if (ad_line == NULL) {
        return NULL;
    }
    ad_line = strtok_r(NULL, split1, &ad_outer_ptr);

    while (ad_line != NULL) {
        char *pkg = strtok_r(ad_line, split2, &ad_inner_ptr);
        if (pkg == NULL) {
            return NULL;
        }
        char *app_name = strtok_r(NULL, split2, &ad_inner_ptr);
        if (app_name == NULL) {
            return NULL;
        }
        char *path = strtok_r(NULL, split2, &ad_inner_ptr);
        if (path == NULL) {
            return NULL;
        }
        char compPath[512] = "";
        strcat(compPath, storage_path);
        strcat(compPath, path);

        if (access(compPath, F_OK) == 0) {
            jstring jpkg = (*env)->NewStringUTF(env, pkg);
            jstring jappName = (*env)->NewStringUTF(env, app_name);
            jstring jtype = (*env)->NewStringUTF(env, app_name);
            jstring jpath = (*env)->NewStringUTF(env, compPath);

            if ((*env)->CallBooleanMethod(env, pkgInstalledSet, setContainId, jpkg)) {
                (*env)->CallBooleanMethod(env, canntDeletePaths, setAddId, jpath);
                (*env)->DeleteLocalRef(env, jpkg);
                (*env)->DeleteLocalRef(env, jappName);
                (*env)->DeleteLocalRef(env, jtype);
                (*env)->DeleteLocalRef(env, jpath);

                ad_line = strtok_r(NULL, split1, &ad_outer_ptr);
                continue;
            }

            if ((*env)->CallBooleanMethod(env, canntDeletePaths, setContainId, jpath)) {
                (*env)->DeleteLocalRef(env, jpkg);
                (*env)->DeleteLocalRef(env, jappName);
                (*env)->DeleteLocalRef(env, jtype);
                (*env)->DeleteLocalRef(env, jpath);

                ad_line = strtok_r(NULL, split1, &ad_outer_ptr);
                continue;
            }

            path_count++;

            long long file_size = get_file_size(compPath);

            jobject appJunk = (*env)->NewObject(env, appJunkClass, appJunkConstruct, jpkg, jappName, file_size, jpath, jtype);

            (*env)->CallVoidMethod(env, appJunk, setInstalledId, False);

            (*env)->CallBooleanMethod(env, appJunkList, addMethodId, appJunk);

            jobject objProgress = (*env)->NewObject(env, progressClass, progressConstruct, path_count, appJunk);

            jobjectArray objProgressArray = (*env)->NewObjectArray(env, 1, progressClass, objProgress);

            (*env)->CallVoidMethod(env, thiz, postProgressMethodId, objProgressArray);

            (*env)->DeleteLocalRef(env, jpkg);
            (*env)->DeleteLocalRef(env, jappName);
            (*env)->DeleteLocalRef(env, jtype);
            (*env)->DeleteLocalRef(env, jpath);
            (*env)->DeleteLocalRef(env, appJunk);
            (*env)->DeleteLocalRef(env, objProgress);
            (*env)->DeleteLocalRef(env, objProgressArray);
        }

        ad_line = strtok_r(NULL, split1, &ad_outer_ptr);
    }

    free(aj_un7z_data);
    free(ad_un7z_data);

    (*env)->DeleteLocalRef(env, ajDecryptData);
    (*env)->DeleteLocalRef(env, adDecryptData);
    (*env)->DeleteLocalRef(env, hashSetClass);
    (*env)->DeleteLocalRef(env, appJunkClass);
    (*env)->DeleteLocalRef(env, processorClass);
    (*env)->DeleteLocalRef(env, arrayListClass);
    (*env)->DeleteLocalRef(env, progressClass);
    (*env)->DeleteLocalRef(env, pkgInstalledSet);
    (*env)->ReleaseStringUTFChars(env, jstoragePath, storage_path);

    return appJunkList;
}

JNIEXPORT jobject JNICALL
Java_com_ihs_device_clean_junk_cache_nonapp_pathrule_task_prs_c( //proguard scanPathFileCache
        JNIEnv *env,
        jobject thiz,
        jbyteArray encryptData, jstring jstoragePath) {

    jclass scanUtilClass = (*env)->FindClass(env, "com/ihs/device/clean/junk/util/SUtils");
    jmethodID decryptMethodId = (*env)->GetStaticMethodID(env, scanUtilClass, "a", "([B[B[B)[B");

    jbyteArray jKey = (*env)->NewByteArray(env, 16);
    jbyteArray jVI = (*env)->NewByteArray(env, 16);

    (*env)->SetByteArrayRegion(env, jKey, 0, 16, (const jbyte *) key);
    (*env)->SetByteArrayRegion(env, jVI, 0, 16, (const jbyte *) iv);

    jobject jdecryptData = (*env)->CallStaticObjectMethod(env, scanUtilClass, decryptMethodId, encryptData, jKey, jVI);

    if (jstoragePath == NULL) {
        LOGE("jstoragePath == NULL");
        return NULL;
    }
    const char *storage_path = (*env)->GetStringUTFChars(env, jstoragePath, 0);

    (*env)->DeleteLocalRef(env, scanUtilClass);
    (*env)->DeleteLocalRef(env, jKey);
    (*env)->DeleteLocalRef(env, jVI);

    if (jdecryptData == NULL) {
        LOGE("jdecryptData == NULL");
        return NULL;
    }

    Byte *decrypt_data = (Byte *) (*env)->GetByteArrayElements(env, jdecryptData, 0);
    int decrypt_data_length = (int) (*env)->GetArrayLength(env, jdecryptData);

    Byte *un7z_data = malloc(1024 * 1024 * 2);

    int pr_un7z_res = extract7zBytes(decrypt_data, decrypt_data_length, un7z_data, storage_path, "pr");
    if (pr_un7z_res != 0){
        return NULL;
    }

    jclass pathRuleClass = (*env)->FindClass(env, "com/ihs/device/clean/junk/cache/nonapp/pathrule/HSPathFileCache");
    jmethodID pathRuleConstruct = (*env)->GetMethodID(env, pathRuleClass, "<init>", "(JLjava/lang/String;Ljava/lang/String;)V");

    jclass arrayListClass = (*env)->FindClass(env, "java/util/ArrayList");
    jmethodID arrayListConstruct = (*env)->GetMethodID(env, arrayListClass, "<init>", "()V");
    jobject objArrayList = (*env)->NewObject(env, arrayListClass, arrayListConstruct, "");
    jmethodID addMethodId = (*env)->GetMethodID(env, arrayListClass, "add", "(Ljava/lang/Object;)Z");

    jclass processorClass = (*env)->FindClass(env, "com/ihs/device/clean/junk/cache/nonapp/pathrule/task/prs");
    jmethodID postProgressMethodId = (*env)->GetMethodID(env, processorClass, "postOnProgressUpdated", "([Ljava/lang/Object;)V");

    jclass progressClass = (*env)->FindClass(env, "com/ihs/device/clean/junk/cache/nonapp/pathrule/task/prsp");
    jmethodID progressConstruct = (*env)->GetMethodID(env, progressClass, "<init>", "(ILcom/ihs/device/clean/junk/cache/nonapp/pathrule/HSPathFileCache;)V");

    const char *split1 = "\n";
    const char *split2 = "\t";

    char *outer_ptr = NULL;
    char *inner_ptr = NULL;

    char *line = strtok_r((char *) un7z_data, split1, &outer_ptr);
    if (line == NULL) {
        return NULL;
    }
    line = strtok_r(NULL, split1, &outer_ptr);

    int existPathCount = 0;

    while (line != NULL) {
        char *type = strtok_r(line, split2, &inner_ptr);
        if (type == NULL) {
            return NULL;
        }
        char *path = strtok_r(NULL, split2, &inner_ptr);
        if (path == NULL) {
            return NULL;
        }
        char compPath[512] = "";
        strcat(compPath, storage_path);
        strcat(compPath, path);

        if (access(compPath, F_OK) == 0) {
            existPathCount++;

            jstring jtype = (*env)->NewStringUTF(env, type);
            jstring jpath = (*env)->NewStringUTF(env, compPath);

            long long file_size = get_file_size(compPath);

            jobject objPathRule = (*env)->NewObject(env, pathRuleClass, pathRuleConstruct, file_size, jpath, jtype);

            (*env)->CallBooleanMethod(env, objArrayList, addMethodId, objPathRule);

            jobject objProgress = (*env)->NewObject(env, progressClass, progressConstruct, existPathCount, objPathRule);

            jobjectArray objProgressArray = (*env)->NewObjectArray(env, 1, progressClass, objProgress);

            (*env)->CallVoidMethod(env, thiz, postProgressMethodId, objProgressArray);

            (*env)->DeleteLocalRef(env, jtype);
            (*env)->DeleteLocalRef(env, jpath);
            (*env)->DeleteLocalRef(env, objPathRule);
            (*env)->DeleteLocalRef(env, objProgress);
            (*env)->DeleteLocalRef(env, objProgressArray);
        }

        line = strtok_r(NULL, split1, &outer_ptr);
    }

    free(un7z_data);

    (*env)->DeleteLocalRef(env, pathRuleClass);
    (*env)->DeleteLocalRef(env, arrayListClass);
    (*env)->DeleteLocalRef(env, processorClass);
    (*env)->DeleteLocalRef(env, progressClass);
    (*env)->ReleaseStringUTFChars(env, jstoragePath, storage_path);

    return objArrayList;
}

long long get_ordinary_file_size(const char *path) {
    long long file_size = -1;
    struct stat stat_buff;
    if (stat(path, &stat_buff) < 0) {
        return file_size;
    } else {
        file_size = stat_buff.st_size;
    }
    return file_size;
}

long long get_file_size(char *base_file_path) {
    long long total_file_size = get_ordinary_file_size(base_file_path);
    DIR *dir;
    struct dirent *ptr;
    char file_path[512] = "";

    if ((dir = opendir(base_file_path)) == NULL) {
        return total_file_size;
    }

    while ((ptr = readdir(dir)) != NULL) {
        if (strcmp(ptr->d_name, ".") == 0 ||
            strcmp(ptr->d_name, "..") == 0) { // current dir OR parrent dir
            continue;
        } else if (ptr->d_type == 8 || ptr->d_type == 10) { // file OR link file
            if (strlen(base_file_path) + strlen("/") + strlen(ptr->d_name) >= 512) {
                closedir(dir);
                return total_file_size;
            }

            strcpy(file_path, base_file_path);
            strcat(file_path, "/");
            strcat(file_path, ptr->d_name);

            total_file_size += get_ordinary_file_size(file_path);
        } else if (ptr->d_type == 4) { // dir
            if (strlen(base_file_path) + strlen("/") + strlen(ptr->d_name) >= 512) {
                closedir(dir);
                return total_file_size;
            }

            strcpy(file_path, base_file_path);
            strcat(file_path, "/");
            strcat(file_path, ptr->d_name);
            total_file_size += get_file_size(file_path);
        }
    }
    closedir(dir);
    return total_file_size;
}