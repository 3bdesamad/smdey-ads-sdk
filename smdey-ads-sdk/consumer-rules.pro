   # ============================================================                                                                                                                                      
    # ProGuard / R8 Consumer Rules for smdey-ads-sdk                                                                                                                                                    
    # Automatically applied to any application using this AAR                                                                                                                                           
    # ============================================================                                                                                                                                      
                                                                                                                                                                                                        
    # 1. Keep all Public SDK APIs, Managers, Builders, and Callbacks                                                                                                                                    
    # This safely preserves AdsSdk, AdsConfig, BannerManager, InterstitialManager,                                                                                                                      
    # RewardedManager (including OnLoadListener), AppOpenManager, ConsentManager, and SdkGate                                                                                                           
    -keep public class com.smdey.ads.sdk.** {
        public *;                                                                                                                                                                                       
    }                                                                                                                                                                                                   
                                                                                                                                                                                                        
    # 2. Keep SmartBannerView (Constructors for XML inflation + Public UI methods + Lifecycle)                                                                                                          
    -keep class com.smdey.ads.sdk.banner.SmartBannerView {
        public <init>(...);                                                                                                                                                                             
        public *;                                                                                                                                                                                       
    }                                                                                                                                                                                                   
                                                                                                                                                                                                        
    # 3. Preserve Generic Type Signatures for Callbacks & Listeners                                                                                                                                     
    -keepattributes Signature                                                                                                                                                                           
    -keepattributes *Annotation*                                                                                                                                                                        
                                                                                                                                                                                                        
    # 4. Google Mobile Ads Next-Gen SDK & UMP                                                                                                                                                           
    # Suppress build-time warnings for optional dependencies and internals                                                                                                                              
    -dontwarn com.google.android.libraries.ads.mobile.sdk.**                                                                                                                                            
    -dontwarn com.google.android.ump.**                                                                                                                                                                 
    -dontwarn com.google.android.gms.internal.ads.**                                                                                                                                                    
                                                                                                                                                                                                        
    # 5. Facebook Shimmer (used for programmatic banner loading skeleton)                                                                                                                               
    -keep class com.facebook.shimmer.** { *; }                                                                                                                                                          
    -dontwarn com.facebook.shimmer.**    