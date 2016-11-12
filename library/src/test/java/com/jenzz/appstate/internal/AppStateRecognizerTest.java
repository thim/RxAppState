package com.jenzz.appstate.internal;

import android.app.Activity;
import android.app.Application;
import android.app.Application.ActivityLifecycleCallbacks;
import android.content.BroadcastReceiver;
import android.content.ComponentCallbacks2;
import android.content.Intent;
import android.content.IntentFilter;

import com.jenzz.appstate.AppStateListener;
import com.jenzz.appstate.dummies.DummyAppStateListener;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import static android.content.ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN;
import static android.content.Intent.ACTION_SCREEN_OFF;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

public class AppStateRecognizerTest {

  @Mock Application mockApplication;
  @Mock AppStateListener mockAppStateListener;

  @Captor ArgumentCaptor<ComponentCallbacks2> componentCallbacksCaptor;
  @Captor ArgumentCaptor<ActivityLifecycleCallbacks> activityCallbacksCaptor;

  private final AppStateRecognizer appStateRecognizer = new AppStateRecognizer();

  @Before
  public void setUp() {
    initMocks(this);
  }

  @Test
  public void registersCallbacks() {
    appStateRecognizer.start(mockApplication, new DummyAppStateListener());

    verify(mockApplication).registerActivityLifecycleCallbacks(any(ActivityLifecycleCallbacks.class));
    verify(mockApplication).registerComponentCallbacks(any(ComponentCallbacks2.class));
    verify(mockApplication).registerReceiver(any(BroadcastReceiver.class), any(IntentFilter.class));
  }

  @Test
  public void unregistersCallbacks() {
    appStateRecognizer.stop(mockApplication);

    verify(mockApplication).unregisterActivityLifecycleCallbacks(any(ActivityLifecycleCallbacks.class));
    verify(mockApplication).unregisterComponentCallbacks(any(ComponentCallbacks2.class));
    verify(mockApplication).unregisterReceiver(any(BroadcastReceiver.class));
  }

  @Test
  public void doesNotReturnNullAppStateByDefault() {
    assertThat(appStateRecognizer.getAppState()).isNotNull();
  }

  @Test
  public void doesNotEmitBackgroundWhenScreenOffAndAppMonitoringJustStarted() {
    appStateRecognizer.start(mockApplication, mockAppStateListener);

    mockApplication.sendBroadcast(new Intent(ACTION_SCREEN_OFF));

    verify(mockAppStateListener, never()).onAppDidEnterBackground();
  }

  @Test
  public void doesNotEmitBackgroundWhenScreenOffAndAppAlreadyOnBackground() {
    appStateRecognizer.start(mockApplication, mockAppStateListener);
    verify(mockApplication).registerComponentCallbacks(componentCallbacksCaptor.capture());
    final ComponentCallbacks2 componentCallbacks = componentCallbacksCaptor.getValue();

    componentCallbacks.onTrimMemory(TRIM_MEMORY_UI_HIDDEN);
    mockApplication.sendBroadcast(new Intent(ACTION_SCREEN_OFF));

    verify(mockAppStateListener, times(1)).onAppDidEnterBackground();
  }

  @Test
  public void emitsForegroundIfActivityStartsOnFirstLaunch() {
    appStateRecognizer.start(mockApplication, mockAppStateListener);
    verify(mockApplication).registerActivityLifecycleCallbacks(activityCallbacksCaptor.capture());

    activityCallbacksCaptor.getValue().onActivityStarted(new Activity());

    verify(mockAppStateListener).onAppDidEnterForeground();
  }

  @Test
  public void doesNotEmitForegroundIfActivityStartsOnSuccessiveLaunches() {
    appStateRecognizer.start(mockApplication, mockAppStateListener);
    verify(mockApplication).registerActivityLifecycleCallbacks(activityCallbacksCaptor.capture());
    final ActivityLifecycleCallbacks lifecycleCallbacks = activityCallbacksCaptor.getValue();
    
    lifecycleCallbacks.onActivityStarted(new Activity());
    lifecycleCallbacks.onActivityStarted(new Activity());
    lifecycleCallbacks.onActivityStarted(new Activity());

    verify(mockAppStateListener, times(1)).onAppDidEnterForeground();
  }
}
