-keep class android.content.pm.** { *; }

-keep class com.ihs.device.clean.junk.util.SUtils { *; }

-keep class com.ihs.device.clean.junk.cache.app.sys.HSAppSysCache { *; }

-keep class com.ihs.device.clean.junk.cache.app.nonsys.data.task.ads { *; }
-keep class com.ihs.device.clean.junk.cache.app.nonsys.data.task.adsp { *; }
-keepclassmembers class com.ihs.device.clean.junk.cache.app.nonsys.data.HSAppDataCache {
    public <init>(java.lang.String, java.lang.String, long, java.lang.String, java.lang.String);
}

-keep class com.ihs.device.clean.junk.cache.app.nonsys.junk.task.ajs { *; }
-keep class com.ihs.device.clean.junk.cache.app.nonsys.junk.task.ajsp { *; }
-keepclassmembers class com.ihs.device.clean.junk.cache.app.nonsys.junk.HSAppJunkCache {
    public <init>(java.lang.String, java.lang.String, long, java.lang.String, java.lang.String);
    public void setInstalled(boolean);
}

-keep class com.ihs.device.clean.junk.cache.nonapp.pathrule.task.prs { *; }
-keep class com.ihs.device.clean.junk.cache.nonapp.pathrule.task.prsp { *; }
-keepclassmembers class com.ihs.device.clean.junk.cache.nonapp.pathrule.HSPathFileCache {
    public <init>(long, java.lang.String, java.lang.String);
}

