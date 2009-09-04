/**
 * This file is part of AudioBoo, an android program for audio blogging.
 * Copyright (C) 2009 BestBefore Media Ltd. All rights reserved.
 *
 * Author: Jens Finkhaeuser <jens@finkhaeuser.de>
 *
 * $Id$
 **/

#include "FLAC/metadata.h"
#include "FLAC/stream_encoder.h"

#include <assert.h>
#include <limits.h>
#include <string.h>
#include <alloca.h>
#include <stdint.h>

#include <jni.h>

namespace {

/*****************************************************************************
 * Constants
 **/
static char const * const FLACStreamEncoder_classname   = "fm.audioboo.jni.FLACStreamEncoder";
static char const * const FLACStreamEncoder_mObject     = "mObject";

static char const * const IllegalArgumentException_classname  = "java.lang.IllegalArgumentException";

static int COMPRESSION_LEVEL                            = 5;



/*****************************************************************************
 * Native FLACStreamEncoder representation
 **/

class FLACStreamEncoder
{
public:
  /**
   * Takes ownership of the outfile.
   **/
  FLACStreamEncoder(char * outfile, int sample_rate, int channels,
      int bits_per_sample)
    : m_outfile(outfile)
    , m_sample_rate(sample_rate)
    , m_channels(channels)
    , m_bits_per_sample(bits_per_sample)
    , m_encoder(NULL)
  {
  }


  /**
   * There are no exceptions here, so we need to "construct" outside the ctor.
   * Returns NULL on success, else an error message
   **/
  char const * const init()
  {
    if (!m_outfile) {
      return "No file name given!";
    }


    // Try to create the encoder instance
    m_encoder = FLAC__stream_encoder_new();
    if (!m_encoder) {
      return "Could not create FLAC__StreamEncoder!";
    }

    // Try to initialize the encoder.
    FLAC__bool ok = true;
    ok &= FLAC__stream_encoder_set_sample_rate(m_encoder, 1.0f * m_sample_rate);
    ok &= FLAC__stream_encoder_set_channels(m_encoder, m_channels);
    ok &= FLAC__stream_encoder_set_bits_per_sample(m_encoder, m_bits_per_sample);
    ok &= FLAC__stream_encoder_set_verify(m_encoder, true);
    ok &= FLAC__stream_encoder_set_compression_level(m_encoder, COMPRESSION_LEVEL);
    if (!ok) {
      return "Could not set up FLAC__StreamEncoder with the given parameters!";
    }

    // Try initializing the file stream.
    FLAC__StreamEncoderInitStatus init_status = FLAC__stream_encoder_init_file(
        m_encoder, m_outfile, NULL, NULL);

    if (FLAC__STREAM_ENCODER_INIT_STATUS_OK != init_status) {
      return "Could not initialize FLAC__StreamEncoder for the given file!";
    }

    return NULL;
  }



  /**
   * Destroys encoder instance, releases outfile
   **/
  ~FLACStreamEncoder()
  {
    if (m_encoder) {
      FLAC__stream_encoder_finish(m_encoder);
      FLAC__stream_encoder_delete(m_encoder);
    }

    if (m_outfile) {
      free(m_outfile);
      m_outfile = NULL;
    }
  }


  /**
   * Copies inbuf to outpuf, assuming that inbuf is really a buffer of
   * sized_sampleT.
   **/
  template <typename sized_sampleT>
  void copyBuffer(FLAC__int32 * outbuf, char * inbuf, int inbufsize)
  {
    for (int i = 0 ; i < inbufsize / sizeof(sized_sampleT) ; ++i) {
      outbuf[i] = reinterpret_cast<sized_sampleT *>(inbuf)[i];
    }
  }



  /**
   * Writes bufsize elements from buffer to the stream. Returns the number of
   * bytes actually written.
   **/
  int write(char * buffer, int bufsize)
  {
    // We have 8 or 16 bit pcm in the buffer, but FLAC expects 32 bit samples,
    // where some of the 32 bits are unused.
    int bufsize32 = bufsize / (m_bits_per_sample / 8);
    FLAC__int32 * buf = reinterpret_cast<FLAC__int32*>(alloca(
          sizeof(FLAC__int32) * bufsize32));

    if (8 == m_bits_per_sample) {
      copyBuffer<int8_t>(buf, buffer, bufsize);
    }
    else if (16 == m_bits_per_sample) {
      copyBuffer<int16_t>(buf, buffer, bufsize);
    }
    else {
      // XXX should never happen, just exit.
      return 0;
    }

    FLAC__bool ok = FLAC__stream_encoder_process_interleaved(m_encoder,
        buf, bufsize32);
    if (!ok) {
      // We don't really know how much was written, we have to assume it was
      // nothing.
      return 0;
    }

    return bufsize;
  }


private:
  // Configuration values passed to ctor
  char *  m_outfile;
  int     m_sample_rate;
  int     m_channels;
  int     m_bits_per_sample;

  // FLAC encoder instance
  FLAC__StreamEncoder * m_encoder;
};




/*****************************************************************************
 * Helper functions
 **/

/**
 * Retrieve FLACStreamEncoder instance from the passed jobject.
 **/
static FLACStreamEncoder * get_encoder(JNIEnv * env, jobject obj)
{
  assert(sizeof(jlong) >= sizeof(FLACStreamEncoder *));

  // Do the JNI dance for getting the mObject field
  jclass cls = env->FindClass(FLACStreamEncoder_classname);
  jfieldID object_field = env->GetFieldID(cls, FLACStreamEncoder_mObject, "J");
  jlong encoder_value = env->GetLongField(obj, object_field);

  env->DeleteLocalRef(cls);

  return reinterpret_cast<FLACStreamEncoder *>(encoder_value);
}


/**
 * Store FLACStreamEncoder instance in the passed jobject.
 **/
static void set_encoder(JNIEnv * env, jobject obj, FLACStreamEncoder * encoder)
{
  assert(sizeof(jlong) >= sizeof(FLACStreamEncoder *));

  // Do the JNI dance for setting the mObject field
  jlong encoder_value = reinterpret_cast<jlong>(encoder);
  jclass cls = env->FindClass(FLACStreamEncoder_classname);
  jfieldID object_field = env->GetFieldID(cls, FLACStreamEncoder_mObject, "J");
  env->SetLongField(obj, object_field, encoder_value);
  env->DeleteLocalRef(cls);
}



/**
 * Convert a jstring to a UTF-8 char pointer. Ownership of the pointer goes
 * to the caller.
 **/
static char * convert_jstring_path(JNIEnv * env, jstring input)
{
  char buf[PATH_MAX];

  jboolean copy = false;
  char const * str = env->GetStringUTFChars(input, &copy);
  if (NULL == str) {
    // OutOfMemoryError has already been thrown here.
    return NULL;
  }

  char * ret = strdup(str);
  env->ReleaseStringUTFChars(input, str);
  return ret;
}


/**
 * Throws the given exception/message
 **/
void throwByName(JNIEnv * env, const char * name, const char * msg)
{
  jclass cls = env->FindClass(name);

  // If cls is NULL, an exception has already been thrown
  if (NULL != cls) {
    env->ThrowNew(cls, msg);
    // Ignore return value of ThrowNew... all we could reasonably do is try and
    // throw another exception, after all.
  }

  env->DeleteLocalRef(cls);
}


} // anonymous namespace



/*****************************************************************************
 * JNI Wrappers
 **/

extern "C" {

void
Java_fm_audioboo_jni_FLACStreamEncoder_init(JNIEnv * env, jobject obj,
    jstring outfile, jint sample_rate, jint channels, jint bits_per_sample)
{
  assert(sizeof(jlong) >= sizeof(FLACStreamEncoder *));

  FLACStreamEncoder * encoder = new FLACStreamEncoder(
      convert_jstring_path(env, outfile), sample_rate, channels,
      bits_per_sample);

  char const * const error = encoder->init();
  if (NULL != error) {
    delete encoder;

    throwByName(env, IllegalArgumentException_classname, error);
    return;
  }

  set_encoder(env, obj, encoder);
}



void
Java_fm_audioboo_jni_FLACStreamEncoder_deinit(JNIEnv * env, jobject obj)
{
  FLACStreamEncoder * encoder = get_encoder(env, obj);
  delete encoder;
  set_encoder(env, obj, NULL);
}



int
Java_fm_audioboo_jni_FLACStreamEncoder_write(JNIEnv * env, jobject obj,
    jbyteArray buffer, jint bufsize)
{
  FLACStreamEncoder * encoder = get_encoder(env, obj);

  if (NULL == encoder) {
    throwByName(env, IllegalArgumentException_classname,
        "Called without a valid encoder instance!");
    return 0;
  }

  jboolean copy = false;
  char * buf = reinterpret_cast<char *>(env->GetByteArrayElements(buffer,
        &copy));

  return encoder->write(buf, bufsize);
}

} // extern "C"
