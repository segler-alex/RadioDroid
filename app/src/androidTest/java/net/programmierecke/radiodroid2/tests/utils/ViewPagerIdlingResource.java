package net.programmierecke.radiodroid2.tests.utils;

import androidx.test.espresso.IdlingResource;
import androidx.viewpager.widget.ViewPager;

public class ViewPagerIdlingResource implements IdlingResource {
    private final String resourceName;

    private boolean isIdle = true;

    private ResourceCallback resourceCallback;

    public ViewPagerIdlingResource(ViewPager viewPager, String name) {
        viewPager.addOnPageChangeListener(new ViewPagerListener());
        resourceName = name;
    }

    @Override
    public String getName() {
        return resourceName;
    }

    @Override
    public boolean isIdleNow() {
        return isIdle;
    }

    @Override
    public void registerIdleTransitionCallback(ResourceCallback resourceCallback) {
        this.resourceCallback = resourceCallback;
    }

    private class ViewPagerListener extends ViewPager.SimpleOnPageChangeListener {

        @Override
        public void onPageScrollStateChanged(int state) {
            isIdle = (state == ViewPager.SCROLL_STATE_IDLE);
            if (isIdle && resourceCallback != null) {
                resourceCallback.onTransitionToIdle();
            }
        }
    }
}
