package com.absolute.facerecognition.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.absolute.facerecognition.R;
import com.absolute.facerecognition.bean.User;

import java.util.List;

public class RetrievalLVAdapter extends BaseAdapter {

    private List<User> users;
    private Context context;

    public RetrievalLVAdapter(Context context, List<User> users) {
        this.context = context;
        this.users = users;
    }

    @Override
    public int getCount() {
        return users.size();
    }

    @Override
    public Object getItem(int position) {
        return users.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        LayoutInflater inflater = LayoutInflater.from(context);
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.retrieval_lv_item, null);
            holder = new ViewHolder();
            /*得到各个控件的对象*/
            holder.userid = (TextView) convertView.findViewById(R.id.retrieval_lv_item_userid);
            holder.score = (TextView) convertView.findViewById(R.id.retrieval_lv_item_score);
            convertView.setTag(holder);//绑定ViewHolder对象
        }else{
            holder = (ViewHolder)convertView.getTag();//取出ViewHolder对象
        }
        /*设置TextView显示的内容，即我们存放在动态数组中的数据*/
        holder.userid.setText("姓名:" + users.get(position).getUser_list().getUser_id());
        holder.score.setText("置信度:" + users.get(position).getUser_list().getScore());
        return convertView;
    }

    class ViewHolder {
        TextView userid;
        TextView score;
    }

}
