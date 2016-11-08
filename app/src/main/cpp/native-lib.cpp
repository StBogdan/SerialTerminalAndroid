

#include <jni.h>
#include <string>
//#include "libusb.h"
extern "C"
jstring
Java_com_comp3215_group1_serialterminal_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}
