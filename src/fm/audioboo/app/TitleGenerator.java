/**
 * This file is part of AudioBoo, an android program for audio blogging.
 * Copyright (C) 2009 BestBefore Media Ltd. All rights reserved.
 *
 * Author: Jens Finkhaeuser <jens@finkhaeuser.de>
 *
 * $Id$
 **/

package fm.audioboo.app;

import java.util.Date;
import java.util.Calendar;
import java.util.GregorianCalendar;

import java.util.Random;
import java.util.LinkedList;

import android.content.Context;

import android.util.Log;

/**
 * Returns a semi-randomly picked Boo title. Titles are defined in localized.xml,
 * and picked based on the time of day and random factors.
 **/
public class TitleGenerator
{
  /***************************************************************************
   * Private constants
   **/
  // Log ID
  private static final String LTAG  = "PublishActivity";


  /***************************************************************************
   * Data
   **/
  // Context to load resources with.
  private Context   mContext;

  // RNG
  private Random    mRNG = new Random();

  // Generic titles
  private String[]  mGeneric;

  // Seasonal
  private String    mSpringboo;
  private String    mSummerboo;
  private String    mHotboo;
  private String    mFallboo;
  private String    mAutumnboo;
  private String    mWinterboo;
  private String    mChillyboo;

  // Time of day.
  private String    mBreakfastboo;
  private String    mMorningboo;
  private String    mLunchboo;
  private String    mAfternoonboo;
  private String    mSunnyboo;
  private String    mTeaboo;
  private String    mEODboo;
  private String    mEveningboo;
  private String    mDarkboo;
  private String    mSleepyboo;

  // Special
  private String    mMeetingsboo;
  private String    mPackedLunchboo;
  private String    mWayHomeboo;
  private String    mHomeboo;
  private String    mPartyboo;
  private String    mDrunkboo;
  private String    mHungOverboo;

  /***************************************************************************
   * Implementation
   **/
  public TitleGenerator(Context ctx)
  {
    mContext = ctx;
  }



  public String getTitle()
  {
    LinkedList<String> selection = new LinkedList<String>();

    // Thresholds taken from iPhone app.
    if (mRNG.nextFloat() > 0.9) {
      addSeasonBoo(selection);
    }
    if (mRNG.nextFloat() > 0.85) {
      addTimedBoo(selection);
    }
    if (mRNG.nextFloat() > 0.75) {
      addSpecialBoo(selection);
    }

    if (0 == selection.size()) {
      return genericBoo();
    }
    return selection.get(Math.abs(mRNG.nextInt()) % selection.size());
  }



  private void addSeasonBoo(LinkedList<String> selection)
  {
    if (null == mSpringboo) {
      mSpringboo = mContext.getResources().getString(R.string.boo_title_spring);
      mSummerboo = mContext.getResources().getString(R.string.boo_title_summer);
      mHotboo = mContext.getResources().getString(R.string.boo_title_hot);
      mFallboo = mContext.getResources().getString(R.string.boo_title_fall);
      mAutumnboo = mContext.getResources().getString(R.string.boo_title_autumn);
      mWinterboo = mContext.getResources().getString(R.string.boo_title_winter);
      mChillyboo = mContext.getResources().getString(R.string.boo_title_chilly);
    }

    GregorianCalendar calendar = new GregorianCalendar();
    calendar.setTime(new Date());

    int month = calendar.get(Calendar.MONTH);
    int hour = calendar.get(Calendar.HOUR_OF_DAY);

    LinkedList<String> sel = new LinkedList<String>();

    if (month >= 3 && month < 6) {
      sel.add(mSpringboo);
    }
    else if (month >= 6 && month < 9) {
      sel.add(mSummerboo);
      if (hour >= 9 && hour <= 20) {
        sel.add(mHotboo);
      }
    }
    else if (month >= 9 && month < 12) {
      sel.add(mFallboo);
      sel.add(mAutumnboo);
    }
    else {
      sel.add(mWinterboo);
      sel.add(mChillyboo);
    }

    selection.add(sel.get(Math.abs(mRNG.nextInt()) % sel.size()));
  }



  private void addTimedBoo(LinkedList<String> selection)
  {
    if (null == mBreakfastboo) {
      mBreakfastboo = mContext.getResources().getString(R.string.boo_title_breakfast);
      mMorningboo = mContext.getResources().getString(R.string.boo_title_morning);
      mLunchboo = mContext.getResources().getString(R.string.boo_title_lunch);
      mAfternoonboo = mContext.getResources().getString(R.string.boo_title_afternoon);
      mSunnyboo = mContext.getResources().getString(R.string.boo_title_sunny);
      mTeaboo = mContext.getResources().getString(R.string.boo_title_tea);
      mEODboo = mContext.getResources().getString(R.string.boo_title_eod);
      mEveningboo = mContext.getResources().getString(R.string.boo_title_evening);
      mDarkboo = mContext.getResources().getString(R.string.boo_title_dark);
      mSleepyboo = mContext.getResources().getString(R.string.boo_title_sleepy);
    }

    GregorianCalendar calendar = new GregorianCalendar();
    calendar.setTime(new Date());

    int hour = calendar.get(Calendar.HOUR_OF_DAY);

    LinkedList<String> sel = new LinkedList<String>();

    // Some values are added more than once to give them more weight.
    if (hour >= 7 && hour < 9) {
      sel.add(mBreakfastboo);
    }
    else if (hour >= 9 && hour < 12) {
      sel.add(mMorningboo);
    }
    else if (hour >= 12 && hour < 15) {
      sel.add(mLunchboo);
      sel.add(mLunchboo);
      sel.add(mSunnyboo);
    }
    else if (hour >= 15 && hour < 18) {
      sel.add(mAfternoonboo);
      sel.add(mAfternoonboo);
      sel.add(mSunnyboo);
    }
    else if (hour >= 18 && hour < 20) {
      sel.add(mTeaboo);
    }
    else if (hour >= 20 && hour < 23) {
      sel.add(mEODboo);
      sel.add(mEveningboo);
    }
    else {
      sel.add(mDarkboo);
      sel.add(mSleepyboo);
    }

    selection.add(sel.get(Math.abs(mRNG.nextInt()) % sel.size()));
  }



  private void addSpecialBoo(LinkedList<String> selection)
  {
    if (null == mMeetingsboo) {
      mMeetingsboo = mContext.getResources().getString(R.string.boo_title_meetings);
      mPackedLunchboo = mContext.getResources().getString(R.string.boo_title_packed_lunch);
      mWayHomeboo = mContext.getResources().getString(R.string.boo_title_way_home);
      mHomeboo = mContext.getResources().getString(R.string.boo_title_home);
      mPartyboo = mContext.getResources().getString(R.string.boo_title_party);
      mDrunkboo = mContext.getResources().getString(R.string.boo_title_drunk);
      mHungOverboo = mContext.getResources().getString(R.string.boo_title_hung_over);
    }


    GregorianCalendar calendar = new GregorianCalendar();
    calendar.setTime(new Date());

    int hour = calendar.get(Calendar.HOUR_OF_DAY);
    int minute = calendar.get(Calendar.MINUTE);
    int weekday = calendar.get(Calendar.DAY_OF_WEEK);

    // Round to nearest hour
    hour = minute > 30 ? (hour + 1) % 24 : hour;

    LinkedList<String> sel = new LinkedList<String>();

    // Work related
    if (weekday >= Calendar.MONDAY && weekday <= Calendar.FRIDAY) {
      if (hour >= 9 && hour < 19) {
        sel.add(mMeetingsboo);
      }
      else if (hour >= 12 && hour < 15) {
        sel.add(mPackedLunchboo);
      }
      else if (hour >= 18 && hour < 20) {
        sel.add(mWayHomeboo);
      }
      else if (hour >= 19 && hour < 21) {
        sel.add(mHomeboo);
      }
    }

    // Party!
    if ((Calendar.FRIDAY == weekday && hour >= 19)
        || (Calendar.SATURDAY == weekday && (hour < 3 || hour >= 19))
        || (Calendar.SUNDAY == weekday && hour < 3))
    {
      sel.add(mPartyboo);
    }

    // Drunk!
    if ((Calendar.FRIDAY == weekday && hour >= 22)
        || (Calendar.SATURDAY == weekday && (hour < 5 || hour >= 22))
        || (Calendar.SUNDAY == weekday && hour < 5))
    {
      sel.add(mDrunkboo);
    }

    // Hungover...
    if ((Calendar.SATURDAY == weekday || Calendar.SUNDAY == weekday)
        && (hour >= 6 && hour < 12))
    {
      sel.add(mHungOverboo);
    }

    selection.add(sel.get(Math.abs(mRNG.nextInt()) % sel.size()));
  }



  private String genericBoo()
  {
    if (null == mGeneric) {
      mGeneric = mContext.getResources().getStringArray(R.array.boo_title_generic);
    }
    return mGeneric[Math.abs(mRNG.nextInt()) % mGeneric.length];
  }
}