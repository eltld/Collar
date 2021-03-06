package com.codez.collar.adapter;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.codez.collar.R;
import com.codez.collar.activity.GroupsMemberActivity;
import com.codez.collar.bean.Group;
import com.codez.collar.databinding.ItemGroupRowBinding;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by codez on 2017/11/21.
 * Description:
 */

public class GroupRowAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = "GroupRowAdapter";
    private Context mContext;
    private List<Group> list;
    private String selectId = null;

    public GroupRowAdapter(Context mContext) {
        this.mContext = mContext;
        this.list = new ArrayList<>();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (mContext == null) {
            mContext = parent.getContext();
        }
        ItemGroupRowBinding mBinding = DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()),
                R.layout.item_group_row, parent, false);
        return new BindingViewHolder(mBinding);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ((BindingViewHolder) holder).bindItem(position);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    class BindingViewHolder extends RecyclerView.ViewHolder{

        private ItemGroupRowBinding mBinding;

        public BindingViewHolder(ItemGroupRowBinding binding) {
            super(binding.getRoot());
            mBinding = binding;
        }

        private void bindItem(final int pos) {
            final Group group = list.get(pos);
            mBinding.setGroup(group);
            mBinding.tvContent.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mContext.startActivity(new Intent(mContext, GroupsMemberActivity.class)
                            .putExtra(GroupsMemberActivity.INTENT_GROUP_ID, group.getId()));
                }
            });
        }
    }

    public void setList(List<Group> list) {
        this.list.clear();
        this.list = list;
    }

    public List<Group> getList() {
        return list;
    }
    public void add(Group bean) {
        this.list.add(bean);
    }
    public void addAll(List<Group> list){
        this.list.addAll(list);
    }
    public void clear() {
        list.clear();
    }
}
