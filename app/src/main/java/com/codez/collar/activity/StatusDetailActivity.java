package com.codez.collar.activity;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;

import com.codez.collar.R;
import com.codez.collar.adapter.UserAlbumAdapter;
import com.codez.collar.auth.AccessTokenKeeper;
import com.codez.collar.base.BaseActivity;
import com.codez.collar.bean.AttitudeResultBean;
import com.codez.collar.bean.CommentBean;
import com.codez.collar.bean.StatusBean;
import com.codez.collar.databinding.ActivityStatusDetailBinding;
import com.codez.collar.event.ToastEvent;
import com.codez.collar.fragment.CommentListFragment;
import com.codez.collar.fragment.RepostListFragment;
import com.codez.collar.manager.AttitudesManager;
import com.codez.collar.net.HttpUtils;
import com.codez.collar.ui.emojitextview.StatusContentTextUtil;
import com.codez.collar.utils.DensityUtil;
import com.codez.collar.utils.EventBusUtils;
import com.codez.collar.utils.JsonUtil;
import com.codez.collar.utils.T;
import com.codez.collar.utils.Tools;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class StatusDetailActivity extends BaseActivity<ActivityStatusDetailBinding> implements View.OnClickListener{

    private static final String TAG = "StatusDetailActivity";
    public static String INTENT_FROM_COMMENT = "from_comment";
    private static final int COMMENT_MAX_LENGTH = 140;
    private boolean isFromComment;

    private StatusBean mBean = null;


    @Override
    public int setContent() {
        return R.layout.activity_status_detail;
    }

    @Override
    public void initView() {
        setToolbarTitle(mBinding.toolbar, "微博正文");

        //获取intent传递过来的bean
        mBean = (StatusBean) getIntent().getSerializableExtra(StatusBean.INTENT_SERIALIZABLE);
        isFromComment = getIntent().getBooleanExtra(INTENT_FROM_COMMENT, false);

        if (isFromComment) {
            mBinding.appbar.setExpanded(false);
        }

        mBinding.setStatus(mBean);
        //微博正文
        mBinding.tvContent.setText(StatusContentTextUtil.getWeiBoContent(mBean.getText(),
                this, mBinding.tvContent));
        //微博图片，根据无图片、多张图片进行不同的显示方式
        setStatusImage(mBinding.recyclerView, mBean.getPic_urls());
        //转发微博体
        if (mBean.getRetweeted_status()==null){
            mBinding.llRetweeted.setVisibility(View.GONE);
        }else{
            //转发微博体的正文
            mBinding.retweetedContent.setText(
                    StatusContentTextUtil.getWeiBoContent(
                            "@" + mBean.getRetweeted_status().getUser().getScreen_name() +
                                    ":" + mBean.getRetweeted_status().getText(),
                            this, mBinding.retweetedContent));
            //转发微博体的图片
            setStatusImage(mBinding.retweetedRecyclerView, mBean.getRetweeted_status().getPic_urls());

            mBinding.llRetweeted.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Bundle mBundle = new Bundle();
                    mBundle.putSerializable(StatusBean.INTENT_SERIALIZABLE, mBean);
                    StatusDetailActivity.this.startActivity(
                            new Intent(StatusDetailActivity.this, StatusDetailActivity.class)
                            .putExtras(mBundle));
                }
            });
        }

        //点赞按钮状态
        int likeState = AttitudesManager.getInstance().getAttitude(mBean.getIdstr());
        if (likeState == -1) {
            HttpUtils.getInstance().getAttitudesService()
                    .existsLike(mBean.getIdstr())
                    .enqueue(new Callback<ResponseBody>() {
                        @Override
                        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                            try {
                                int state = JsonUtil.getValueByUncertainKey(response.body().bytes());
                                AttitudesManager.getInstance().putAttitude(mBean.getIdstr(), state);
                                mBinding.ivLike.setSelected(state == AttitudesManager.STATE_LIKE);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onFailure(Call<ResponseBody> call, Throwable t) {
                            Log.i(TAG, "onFailure:" + t.toString());
                        }
                    });
        }else{
            mBinding.ivLike.setSelected(likeState == AttitudesManager.STATE_LIKE);
        }
        //设置评论和转发列表
        mBinding.viewPager.setAdapter(new FragmentPagerAdapter(getSupportFragmentManager()) {
            String[] titles = {"评论 "+ mBean.getComments_count(), "转发 "+ mBean.getReposts_count()};
            @Override
            public Fragment getItem(int position) {
                if (position == 0) {
                    return new CommentListFragment().newInstance(mBean.getId(), CommentListFragment.TYPE_COMMENT_STATUS_DETAIL);
                }else{
                    return new RepostListFragment().newInstance(mBean.getId());
                }
            }

            @Override
            public int getCount() {
                return titles.length;
            }

            @Override
            public CharSequence getPageTitle(int position) {
                return titles[position];
            }
        });
        mBinding.tabLayout.setupWithViewPager(mBinding.viewPager);

        //设置edittext
        mBinding.etContent.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if ("".equals(s.toString())) {
                    mBinding.ivCommit.setSelected(false);
                }else{
                    mBinding.ivCommit.setSelected(true);
                }
                mBinding.tvStatusLength.setText((COMMENT_MAX_LENGTH - s.length())+"");
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        mBinding.etContent.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    mBinding.rlAdditional.setVisibility(View.VISIBLE);
                }else{
                    mBinding.rlAdditional.setVisibility(View.GONE);
                }
            }
        });

        mBinding.blockLike.setOnClickListener(this);
        mBinding.blockComment.setOnClickListener(this);
        mBinding.blockRepost.setOnClickListener(this);
        mBinding.ivCommit.setOnClickListener(this);

        reloadData();
    }

    private void reloadData() {
        HttpUtils.getInstance().getWeiboService()
                .getStatusInfo(mBean.getIdstr())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<StatusBean>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.w(TAG, "onError:" + e.toString());
                    }

                    @Override
                    public void onNext(StatusBean statusBean) {
                        mBinding.tabLayout.getTabAt(0).setText("评论 " + statusBean.getComments_count());
                        mBinding.tabLayout.getTabAt(1).setText("转发 " + statusBean.getReposts_count());
                    }
                });
    }

    private void setStatusImage(RecyclerView recyclerView, ArrayList<StatusBean.PicUrlsBean> pic_urls) {
        if (pic_urls.size() == 0) {
            recyclerView.setVisibility(View.GONE);
            return;
        } else {
            recyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        }
        recyclerView.setNestedScrollingEnabled(false);
        UserAlbumAdapter mAdapter = new UserAlbumAdapter(this);
        recyclerView.setAdapter(mAdapter);
        recyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
            int itemPadding = DensityUtil.dp2px(StatusDetailActivity.this, 4);

            @Override
            public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
                super.onDraw(c, parent, state);
            }

            @Override
            public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
                outRect.bottom = itemPadding;
                outRect.left = itemPadding;
                outRect.right = itemPadding;
                outRect.top = itemPadding;
            }
        });
        mAdapter.addAll(pic_urls);
        mAdapter.notifyDataSetChanged();
    }
    private void createComment() {
        //对微博内容长度进行判断，超过STATUS_MAX_LENGTH（140）则不予发送
        if (mBinding.etContent.length() > COMMENT_MAX_LENGTH) {
            T.s(this,"评论无法发送，内容长度超过140个字符！");
            return;
        }
        //发送评论内容
        HttpUtils.getInstance().getCommentService()
                .createComment(AccessTokenKeeper.getInstance().getAccessToken(), mBinding.etContent.getText().toString(), mBean.getIdstr(),0)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<CommentBean>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        T.s(StatusDetailActivity.this, "操作失败");
                    }

                    @Override
                    public void onNext(CommentBean commentBean) {
                        T.s(StatusDetailActivity.this, "评论成功");
                        mBinding.etContent.setText("");
                        mBinding.etContent.clearFocus();
                        if (getWindow().getAttributes().softInputMode == WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE) {
                            Tools.hiddenKeyboard(StatusDetailActivity.this);
                        }
                        //TODO:刷新当前评论列表，显示刚提交的评论item

                    }
                });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.block_like:
                if (mBinding.ivLike.isSelected()) {
                    HttpUtils.getInstance().getAttitudesService()
                            .destroyLike(AccessTokenKeeper.getInstance().getAccessToken(), mBean.getIdstr())
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new Observer<AttitudeResultBean>() {
                                @Override
                                public void onCompleted() {
                                    Log.i(TAG, "onCompleted");
                                }

                                @Override
                                public void onError(Throwable e) {
                                    Log.e(TAG, "onError:" + e.toString());
                                }

                                @Override
                                public void onNext(AttitudeResultBean attitudeResultBean) {
                                    EventBusUtils.sendEvent(ToastEvent.newToastEvent("取消点赞"));
                                    AttitudesManager.getInstance().putAttitude(mBean.getIdstr(), 0);
                                    mBinding.ivLike.setSelected(false);
                                    mBinding.ivLike.startAnimation(AnimationUtils.loadAnimation(StatusDetailActivity.this, R.anim.anim_like_scale));
                                    mBean.setAttitudes_count(mBean.getAttitudes_count() - 1);
                                    mBinding.tvLikeCount.setText(String.valueOf(mBean.getAttitudes_count()));
                                }
                            });
                } else {
                    HttpUtils.getInstance().getAttitudesService()
                            .createLike(AccessTokenKeeper.getInstance().getAccessToken(), mBean.getIdstr())
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new Observer<AttitudeResultBean>() {
                                @Override
                                public void onCompleted() {
                                    Log.i(TAG, "onCompleted");
                                }

                                @Override
                                public void onError(Throwable e) {
                                    Log.e(TAG, "onError:" + e.toString());
                                }

                                @Override
                                public void onNext(AttitudeResultBean attitudeResultBean) {
                                    EventBusUtils.sendEvent(ToastEvent.newToastEvent("点赞"));
                                    AttitudesManager.getInstance().putAttitude(mBean.getIdstr(), 1);
                                    mBinding.ivLike.setSelected(true);
                                    mBinding.ivLike.startAnimation(AnimationUtils.loadAnimation(StatusDetailActivity.this, R.anim.anim_like_scale));
                                    mBean.setAttitudes_count(mBean.getAttitudes_count() + 1);
                                    mBinding.tvLikeCount.setText(String.valueOf(mBean.getAttitudes_count()));
                                }
                            });
                }
                break;
            case R.id.block_comment:
                break;
            case R.id.block_repost:
                break;
            case R.id.iv_commit:
                if (mBinding.ivCommit.isSelected()) {
                    createComment();
                }
                break;
        }
    }

}
