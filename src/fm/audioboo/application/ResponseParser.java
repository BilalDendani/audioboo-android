/**
 * This file is part of AudioBoo, an android program for audio blogging.
 * Copyright (C) 2009 BestBefore Media Ltd. All rights reserved.
 *
 * Author: Jens Finkhaeuser <jens@finkhaeuser.de>
 *
 * $Id$
 **/

package fm.audioboo.application;

import android.os.Handler;

import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.LinkedList;

import android.util.Log;

/**
 * Helper class for parsing API responses.
 **/
class ResponseParser
{
  /***************************************************************************
   * Response object.
   **/
  public static class Response<T>
  {
    public int  mTimestamp;
    public int  mWindow;
    public T    mContent;
  };


  /***************************************************************************
   * Private constants
   **/
  // Log ID
  private static final String LTAG  = "ResponseParser";

  // Expected response version. In the future, we may want to handle more than
  // one version and add support for several.
  private static final int EXPECTED_VERSION           = API.API_VERSION;

  // Keys found in the JSON response
  private static final String VERSION                 = "version";
  private static final String BODY                    = "body";

  private static final String ERROR                   = "error";
  private static final String ERROR_CODE              = "code";
  private static final String ERROR_DESCRIPTION       = "description";

  private static final String WINDOW                  = "window";
  private static final String TOTALS                  = "totals";
  private static final String TOTALS_OFFSET           = "offset";
  private static final String TOTALS_COUNT            = "count";
  private static final String TIMESTAMP               = "timestamp";
  private static final String AUDIO_CLIPS             = "audio_clips";

  private static final String USER                    = "user";
  private static final String USER_ANONYMOUS          = "anonymous";
  private static final String USER_URLS               = "urls";
  private static final String USER_URLS_PROFILE       = "profile";
  private static final String USER_URLS_IMAGE         = "image";
  private static final String USER_USERNAME           = "username";
  private static final String USER_ID                 = "id";
  private static final String USER_COUNTS             = "counts";
  private static final String USER_COUNTS_FOLLOWERS   = "followers";
  private static final String USER_COUNTS_AUDIO_CLIPS = "audio_clips";
  private static final String USER_COUNTS_FOLLOWINGS  = "followings";

  private static final String BOO_ID                  = "id";
  private static final String BOO_TITLE               = "title";
  private static final String BOO_URLS                = "urls";
  private static final String BOO_URLS_HIGH_MP3       = "high_mp3";
  private static final String BOO_URLS_IMAGE          = "image";
  private static final String BOO_URLS_DETAIL         = "detail";
  private static final String BOO_UPLOADED_AT         = "uploaded_at";
  private static final String BOO_RECORDED_AT         = "recorded_at";
  private static final String BOO_COUNTS              = "counts";
  private static final String BOO_COUNTS_PLAYS        = "plays";
  private static final String BOO_COUNTS_COMMENTS     = "comments";
  private static final String BOO_DURATION            = "duration";

  private static final String LOCATION                = "location";
  private static final String LOCATION_LONGITUDE      = "longitude";
  private static final String LOCATION_LATITUDE       = "latitude";
  private static final String LOCATION_ACCURACY       = "accuracy";
  private static final String LOCATION_DESCRIPTION    = "description";

  private static final String TAGS                    = "tags";
  private static final String TAG_DISPLAY             = "display_tag";
  private static final String TAG_NORMALISED          = "normalised_tag";
  private static final String TAG_URL                 = "url";

  // Fields for registration responses
  private static final String SOURCE                  = "source";
  private static final String API_SECRET              = "api_secret";
  private static final String API_KEY                 = "api_key";

  // Fields for status responses
  private static final String STATUS_LINKED           = "linked";
  private static final String STATUS_LINK_URL         = "link_url";
  private static final String STATUS_ACCOUNT          = "account";
  private static final String STATUS_ACC_USERNAME     = "username";
  private static final String STATUS_ACC_EMAIL        = "email";

  // Unlink response
  private static final String UNLINKED                = "unlinked";

  // Upload response
  private static final String AUDIO_CLIP              = "audio_clip";
  private static final String CLIP_ID                 = "id";

  /***************************************************************************
   * Implementation
   **/

  /**
   * Returns a filled BooList or null. If null is returned, the Handler will
   * have been sent an error code from the API.ERR_* list.
   **/
  public static Response<BooList> parseBooList(String response, Handler handler)
  {
    try {
      Response<JSONObject> body = retrieveBody(response, handler);
      if (null == body) {
        return null;
      }

      BooList list = new BooList();

      JSONObject totals = body.mContent.getJSONObject(TOTALS);
      list.mOffset = totals.getInt(TOTALS_OFFSET);
      list.mCount = totals.getInt(TOTALS_COUNT);

      // Read metadata for individual boos
      JSONArray boos = body.mContent.getJSONArray(AUDIO_CLIPS);
      for (int i = 0 ; i < boos.length() ; ++i) {
        list.mClips.add(parseBoo(boos.getJSONObject(i)));
      }
      // Log.d(LTAG, "# clips: " + list.mClips.size());

      Response<BooList> result = new Response<BooList>();
      result.mTimestamp = body.mTimestamp;
      result.mWindow = body.mWindow;
      result.mContent = list;
      return result;

    } catch (JSONException ex) {
      Log.e(LTAG, "Could not parse JSON response: " + ex);
      handler.obtainMessage(API.ERR_PARSE_ERROR).sendToTarget();
      return null;
    }
  }



  /**
   * Parses a registration response, returns the secret in Pair's first
   * member, and the key in the second.
   **/
  public static Response<Pair<String, String>> parseRegistrationResponse(String response, Handler handler)
  {
    try {
      Response<JSONObject> body = retrieveBody(response, handler);
      if (null == body) {
        return null;
      }

      JSONObject source = body.mContent.getJSONObject(SOURCE);

      Response<Pair<String, String>> result = new Response<Pair<String, String>>();
      result.mTimestamp = body.mTimestamp;
      result.mWindow = body.mWindow;
      result.mContent = new Pair<String, String>(
          source.getString(API_SECRET),
          source.getString(API_KEY));

      return result;

    } catch (JSONException ex) {
      Log.e(LTAG, "Could not parse JSON response: " + ex);
      handler.obtainMessage(API.ERR_PARSE_ERROR).sendToTarget();
      return null;
    }
  }



  /**
   * Returns true if the response contains the "unlinked" field and that is
   * set to "true". Otherwise returns false.
   **/
  public static Response<Boolean> parseUnlinkResponse(String response, Handler handler)
  {
    try {
      Response<JSONObject> body = retrieveBody(response, handler);
      if (null == body) {
        return null;
      }

      Response<Boolean> result = new Response<Boolean>();
      result.mTimestamp = body.mTimestamp;
      result.mWindow = body.mWindow;
      result.mContent = body.mContent.getBoolean(UNLINKED);
      return result;

    } catch (JSONException ex) {
      Log.e(LTAG, "Could not parse JSON response: " + ex);
      handler.obtainMessage(API.ERR_PARSE_ERROR).sendToTarget();
      return null;
    }
  }



  /**
   * Parses the response to API_STATUS requests.
   **/
  public static Response<API.Status> parseStatusResponse(String response, Handler handler)
  {
    try {
      Response<JSONObject> body = retrieveBody(response, handler);
      if (null == body) {
        return null;
      }

      // First, figure out if the device is linked. That determines the
      // remainder of the fields we can expect.
      API.Status status = new API.Status();
      status.mLinked = body.mContent.getBoolean(STATUS_LINKED);

      if (status.mLinked) {
        // There should be an account field with a username and email.
        JSONObject account = body.mContent.getJSONObject(STATUS_ACCOUNT);

        status.mUsername = account.getString(STATUS_ACC_USERNAME);
        status.mEmail = account.getString(STATUS_ACC_EMAIL);
      }
      else {
        // The only thing we'll find if the device is not linked is a
        // link url.
        status.mLinkUri = Uri.parse(body.mContent.getString(STATUS_LINK_URL));
      }

      Response<API.Status> result = new Response<API.Status>();
      result.mTimestamp = body.mTimestamp;
      result.mWindow = body.mWindow;
      result.mContent = status;
      return result;

    } catch (JSONException ex) {
      Log.e(LTAG, "Could not parse JSON response: " + ex);
      handler.obtainMessage(API.ERR_PARSE_ERROR).sendToTarget();
      return null;
    }
  }



  /**
   * Parse the response to an upload request. It should only contain the ID of
   * the newly uploaded clip.
   **/
  public static Response<Integer> parseUploadResponse(String response, Handler handler)
  {
    try {
      Response<JSONObject> body = retrieveBody(response, handler);
      if (null == body) {
        return null;
      }

      JSONObject clip = body.mContent.getJSONObject(AUDIO_CLIP);

      Response<Integer> result = new Response<Integer>();
      result.mTimestamp = body.mTimestamp;
      result.mWindow = body.mWindow;
      result.mContent = clip.getInt(CLIP_ID);
      return result;

    } catch (JSONException ex) {
      Log.e(LTAG, "Could not parse JSON response: " + ex);
      handler.obtainMessage(API.ERR_PARSE_ERROR).sendToTarget();
      return null;
    }
  }



  private static Date parseTimestamp(String str)
  {
    SimpleDateFormat format = API.ISO8601Format();
    try {
      return format.parse(str);
    } catch (ParseException ex) {
      Log.e(LTAG, "Could not parse timestamp: " + str);
      return null;
    }
  }



  private static Boo parseBoo(JSONObject boo) throws JSONException
  {
    Boo result = new Boo();

    // Parse user data
    result.mUser = parseUser(boo.getJSONObject(USER));

    // Parse location data
    if (boo.has(LOCATION)) {
      result.mLocation = parseLocation(boo.getJSONObject(LOCATION));
    }

    // Parse Boo metadata
    result.mId  = boo.getInt(BOO_ID);
    result.mTitle = boo.getString(BOO_TITLE);

    result.mDuration = boo.getDouble(BOO_DURATION);

    // Parse tags
    result.mTags = parseTags(boo.getJSONArray(TAGS));

    // Timestamps
    result.mRecordedAt = parseTimestamp(boo.getString(BOO_RECORDED_AT));
    result.mUploadedAt = parseTimestamp(boo.getString(BOO_UPLOADED_AT));

    // URLs
    JSONObject urls = boo.getJSONObject(BOO_URLS);
    result.mHighMP3Url = Uri.parse(urls.getString(BOO_URLS_HIGH_MP3));
    result.mDetailUrl = Uri.parse(urls.getString(BOO_URLS_DETAIL));
    if (urls.has(BOO_URLS_IMAGE)) {
      result.mImageUrl = Uri.parse(urls.getString(BOO_URLS_IMAGE));
    }

    // Stats
    JSONObject stats = boo.getJSONObject(BOO_COUNTS);
    result.mPlays = stats.getInt(BOO_COUNTS_PLAYS);
    result.mComments = stats.getInt(BOO_COUNTS_COMMENTS);

    // Log.d(LTAG, "result: " + result);

    return result;
  }



  private static User parseUser(JSONObject user) throws JSONException
  {
    if (user.has(USER_ANONYMOUS) && user.getBoolean(USER_ANONYMOUS)) {
      return null;
    }

    User result = new User();

    // Basic information
    result.mId = user.getInt(USER_ID);
    result.mUsername = user.getString(USER_USERNAME);

    // Urls
    JSONObject urls = user.getJSONObject(USER_URLS);
    result.mProfileUrl = Uri.parse(urls.getString(USER_URLS_PROFILE));
    result.mImageUrl = Uri.parse(urls.getString(USER_URLS_IMAGE));

    // Stats
    JSONObject counts = user.getJSONObject(USER_COUNTS);
    result.mFollowers = counts.getInt(USER_COUNTS_FOLLOWERS);
    result.mFollowings = counts.getInt(USER_COUNTS_FOLLOWINGS);
    result.mAudioClips = counts.getInt(USER_COUNTS_AUDIO_CLIPS);

//    Log.d(LTAG, "User: " + result);

    return result;
  }



  private static BooLocation parseLocation(JSONObject location) throws JSONException
  {
    BooLocation result = new BooLocation();

    result.mLongitude = location.getDouble(LOCATION_LONGITUDE);
    result.mLatitude = location.getDouble(LOCATION_LATITUDE);
    result.mAccuracy = location.getDouble(LOCATION_ACCURACY);

    result.mDescription = location.getString(LOCATION_DESCRIPTION);

    // Log.d(LTAG, "Location: " + result);

    return result;
  }



  private static LinkedList<Tag> parseTags(JSONArray tags) throws JSONException
  {
    if (0 == tags.length()) {
      return null;
    }

    LinkedList<Tag> result = new LinkedList<Tag>();

    for (int i = 0 ; i < tags.length() ; ++i) {
      result.add(parseTag(tags.getJSONObject(i)));
    }

    return result;
  }



  private static Tag parseTag(JSONObject tag) throws JSONException
  {
    Tag result = new Tag();

    result.mDisplay = tag.getString(TAG_DISPLAY);
    result.mNormalised = tag.getString(TAG_NORMALISED);
    result.mUrl = Uri.parse(tag.getString(TAG_URL));

    // Log.d(LTAG, "Tag: " + result);

    return result;
  }



  /**
   * Convenience function: checks the returned version, and checks for errors.
   * Returns a JSONObject representing the response body on success, null
   * on failure. If null is returned, the Handler has already been sent an
   * appropriate error message.
   **/
  private static Response<JSONObject> retrieveBody(String response, Handler handler) throws JSONException
  {
    JSONObject object = new JSONObject(response);

    Response<JSONObject> result = new Response<JSONObject>();

    // Parse metadata.
    result.mWindow = object.getInt(WINDOW);
    result.mTimestamp = object.getInt(TIMESTAMP);

    if (!checkVersion(object, handler)) {
      return null;
    }

    result.mContent = object.getJSONObject(BODY);

    if (!handleError(result.mContent, handler)) {
      return null;
    }

    return result;
  }



  /**
   * Returns true if no error has been reported, otherwise returns false.
   * If false is returned, the handler has already been sent a ERR_API_ERROR
   * message.
   **/
  private static boolean handleError(JSONObject body, Handler handler) throws JSONException
  {
    if (!body.has(ERROR)) {
      return true;
    }

    JSONObject error = body.getJSONObject(ERROR);
    int code = error.getInt(ERROR_CODE);
    String message = error.getString(ERROR_DESCRIPTION);

    Log.e(LTAG, "Error response [" + code + "]: " + message);
    handler.obtainMessage(API.ERR_API_ERROR,
        new API.APIException(message, code)).sendToTarget();

    return false;
  }



  /**
   * Returns true if the response version matches the expected version, else
   * false. If false is returned, the handler has already been sent a
   * ERR_VERSION_MISMATCH message.
   **/
  private static boolean checkVersion(JSONObject object, Handler handler) throws JSONException
  {
    // Check version of the response first. We expect a particular version
    // at the moment.
    int version = object.getInt(VERSION);
    if (EXPECTED_VERSION != version) {
      Log.e(LTAG, "Response version did not match our expectations.");
      handler.obtainMessage(API.ERR_VERSION_MISMATCH).sendToTarget();
      return false;
    }

    return true;
  }
}