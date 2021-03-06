package com.codez.collar.activity;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.ScaleAnimation;

import com.codez.collar.R;
import com.codez.collar.base.BaseActivity;
import com.codez.collar.bean.AlbumsBean;
import com.codez.collar.databinding.ActivityImageDetailBinding;
import com.codez.collar.fragment.ImageDetailFragment;
import com.codez.collar.ui.IndicatorView;
import com.codez.collar.utils.ScreenUtil;

public class ImageDetailActivity extends BaseActivity<ActivityImageDetailBinding> {

    private static final String TAG = "ImageDetailActivity";
    public static final String INTENT_KEY_URL = "url";

    @Override
    public int setContent() {
        return R.layout.activity_image_detail;
    }

    @Override
    public void initView() {
        setSwipeBackEnable(false);
        startZoomAnim();

        final AlbumsBean bean = (AlbumsBean) getIntent().getSerializableExtra(AlbumsBean.INTENT_SERIALIZABLE);

        final int size = bean.getPic_urls().size();

        if (size == 1 || size > 9) {
            mBinding.indicatorView.setVisibility(View.GONE);
        }else{
            mBinding.indicatorView.init(bean.getPic_urls().size(), bean.getCurPosition(), IndicatorView.THEME_DARK);
        }

        mBinding.viewPager.setAdapter(new FragmentPagerAdapter(getSupportFragmentManager()) {

            @Override
            public Fragment getItem(int position) {
                return new ImageDetailFragment().newInstance(bean.getPic_urls().get(position).getThumbnail_pic());
            }

            @Override
            public int getCount() {
                return size;
            }

            @Override
            public CharSequence getPageTitle(int position) {
                return "#";
            }
        });
        mBinding.viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                mBinding.indicatorView.playTo(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        mBinding.viewPager.setCurrentItem(bean.getCurPosition());
        mBinding.tabLayout.setupWithViewPager(mBinding.viewPager);

    }

    private void startZoomAnim() {
        if (savedInstanceState == null) {
            final int x = getIntent().getIntExtra("locationX", 100);
            final int y = getIntent().getIntExtra("locationY", 100);
            mBinding.llRoot.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    mBinding.llRoot.getViewTreeObserver().removeOnPreDrawListener(this);
                    ScaleAnimation animation = new ScaleAnimation(0.3f, 1.0f, 0.3f, 1.0f, x, y);
                    animation.setDuration(200);
                    mBinding.llRoot.startAnimation(animation);
                    return true;
                }
            });
        }
    }

    @Override
    public void finish() {
        AnimatorSet animatorSet = new AnimatorSet();
        ObjectAnimator animator = ObjectAnimator.ofFloat(mBinding.llRoot, "alpha", 1.0f, 0f);
        ObjectAnimator animator1 = ObjectAnimator.ofFloat(mBinding.viewPager, "translationY", 0f, ScreenUtil.getScreenHeight(ImageDetailActivity.this)*1.0f);
        animatorSet.play(animator).with(animator1);
        animatorSet.setDuration(200);
        animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                ImageDetailActivity.super.finish();
                overridePendingTransition(0, 0);
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        animatorSet.start();

    }
}
