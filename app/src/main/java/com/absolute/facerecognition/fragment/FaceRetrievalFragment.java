package com.absolute.facerecognition.fragment;


import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.absolute.facerecognition.GetPhotoFromPhotoAlbum;
import com.absolute.facerecognition.R;
import com.absolute.facerecognition.adapter.RetrievalLVAdapter;
import com.absolute.facerecognition.bean.User;
import com.alibaba.fastjson.JSON;
import com.bumptech.glide.Glide;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import pub.devrel.easypermissions.EasyPermissions;
import pub.devrel.easypermissions.PermissionRequest;

import static android.app.Activity.RESULT_OK;

/**
 * 人脸检索Fragment
 */
public class FaceRetrievalFragment extends Fragment implements View.OnClickListener, EasyPermissions.PermissionCallbacks {

    Button retrieval_btn_camera;
    Button retrieval_btn_photo;
    Button retrieval_btn_retrieval;
    ImageView retrieval_iv_img;
    ListView retrieval_lv_users;
    LinearLayout retrieval_ll;

    private View view;

    private ProgressDialog dialog;

    private File cameraSavePath;
    private Uri uri;

    private final String DETECT_FACE_URL = "http://192.168.1.106:3030/search";

    // 相机权限和SD卡写入权限
    private final String[] PERMS = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.INTERNET};
    // 初始权限请求码
    private final int INIT_REQUESTCODE = 2000;
    // 相机相册请求码
    private final int REQUEST_CAMERA_CODE = 1;
    private final int REQUEST_ALBUM_CODE = 2;

    public FaceRetrievalFragment() {

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (view == null) {
            view = inflater.inflate(R.layout.fragment_face_retrieval, container, false);
        }
        initView();
        initPermissions();
        return view;
    }

    private void initView() {
        retrieval_btn_camera = view.findViewById(R.id.retrieval_btn_camera);
        retrieval_btn_photo = view.findViewById(R.id.retrieval_btn_photo);
        retrieval_btn_retrieval = view.findViewById(R.id.retrieval_btn_retrieval);
        retrieval_lv_users = view.findViewById(R.id.retrieval_lv_users);
        retrieval_iv_img = view.findViewById(R.id.retrieval_iv_img);
        retrieval_ll = view.findViewById(R.id.retrieval_ll);
        retrieval_btn_camera.setOnClickListener(this);
        retrieval_btn_photo.setOnClickListener(this);
        retrieval_btn_retrieval.setOnClickListener(this);
    }

    private void initPermissions(){
        if(!EasyPermissions.hasPermissions(getContext(), PERMS)) {
            // 没有权限,进行申请权限
            EasyPermissions.requestPermissions(new PermissionRequest.Builder(this, INIT_REQUESTCODE, PERMS).build());
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.retrieval_btn_camera:
                // 判断是否有相机权限
                if (EasyPermissions.hasPermissions(getContext(), new String[] {PERMS[0]})) {
                    //调用相机
                    startCamera();
                } else {
                    initPermissions();
                }
                break;
            case R.id.retrieval_btn_photo:
                // 判断是否有相册权限
                if (EasyPermissions.hasPermissions(getContext(), new String[] {PERMS[1]})) {
                    getAlbum();
                } else {
                    initPermissions();
                }
                break;
            case R.id.retrieval_btn_retrieval:
                if (EasyPermissions.hasPermissions(getContext(), new String[] {PERMS[2]})) {
                    uploadImage();
                } else {
                    initPermissions();
                }
                break;
            default:
                break;
        }
    }

    private void startCamera() {
        cameraSavePath = new File(Environment.getExternalStorageDirectory().getPath() + "/" + System.currentTimeMillis() + ".jpg");
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            //第二个参数为 包名.fileprovider
            uri = FileProvider.getUriForFile(getContext(), "com.absolute.fileprovider", cameraSavePath);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            uri = Uri.fromFile(cameraSavePath);
        }
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        startActivityForResult(intent, REQUEST_CAMERA_CODE);
    }

    private void getAlbum() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_ALBUM_CODE);
    }

    private void uploadImage() {
        if (cameraSavePath == null || !cameraSavePath.exists()) {
            Toast.makeText(getContext(), "请先选择人脸识别图片", Toast.LENGTH_SHORT).show();
        } else {
            dialog = new ProgressDialog(getContext());
            dialog.setTitle("识别中...");
            dialog.show();
            OkHttpClient client = new OkHttpClient.Builder()
                    .build();
            // 设置文件以及文件上传类型封装
            RequestBody requestBody = RequestBody.create(MediaType.parse("image/jpg"), cameraSavePath);
            // 文件上传的请求体封装
            MultipartBody multipartBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("key", "csh")
                    .addFormDataPart("file", cameraSavePath.getName(), requestBody)
                    .build();
            final Request request = new Request.Builder()
                    .url(DETECT_FACE_URL)
                    .post(multipartBody)
                    .build();
            Call call = client.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(@NotNull Call call, @NotNull final IOException e) {
                    Log.e("TAG", "ERROR!     " + e.toString());
                    if (dialog != null && dialog.isShowing()) {
                        dialog.dismiss();
                    }
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getContext(), "网络错误" + e.toString(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                    if (dialog != null && dialog.isShowing()) {
                        dialog.dismiss();
                    }
                    String responseString = response.body().string();
                    Log.e("TAG", "responseString:" + responseString);
                    try {
                        JSONObject responseJson = new JSONObject(responseString);
                        String errorMsg = responseJson.getString("error_msg");
                        if (errorMsg != null && errorMsg.equals("SUCCESS")) {
                            Log.e("TAG", "SUCCESS");
                            String resultString = responseJson.getString("result");
                            JSONObject resultJson = new JSONObject(resultString);
                            int face_num = Integer.valueOf(resultJson.getString("face_num"));
                            if (face_num == 0) {
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(getContext(), "未检索到库中人脸", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            } else {
                                String faceList = resultJson.getString("face_list");
                                String faceListR = faceList.substring(1, faceList.length() - 1);
                                faceListR = faceListR.replace("[", "");
                                faceListR = faceListR.replace("]", "");
                                StringBuilder sb = new StringBuilder(faceListR);
                                sb.insert(0, "[");
                                sb.insert(sb.length(), "]");
                                faceList = sb.toString();
                                Log.e("TAG", "face_list=" + faceList);
                                List<User> users = JSON.parseArray(faceList, User.class);
                                final RetrievalLVAdapter adapter = new RetrievalLVAdapter(getContext(), users);
                                // 隐藏按钮
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        retrieval_ll.setVisibility(View.GONE);
                                        retrieval_lv_users.setVisibility(View.VISIBLE);
                                        retrieval_lv_users.setAdapter(adapter);
                                    }
                                });
                            }
                        } else {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(getContext(), "网络错误", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        String photoPath;
        if (requestCode == REQUEST_CAMERA_CODE && resultCode == RESULT_OK) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                photoPath = String.valueOf(cameraSavePath);
            } else {
                photoPath = uri.getEncodedPath();
            }
            Glide.with(getContext()).load(photoPath).into(retrieval_iv_img);
        } else if (requestCode == REQUEST_ALBUM_CODE && resultCode == RESULT_OK) {
            photoPath = GetPhotoFromPhotoAlbum.getRealPathFromUri(getContext(), data.getData());
            cameraSavePath = new File(photoPath);
            Glide.with(getContext()).load(photoPath).into(retrieval_iv_img);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == INIT_REQUESTCODE){
            EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
        }
    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {

    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
        Toast.makeText(getContext(), "需要打开相应权限才能使用相关功能哦", Toast.LENGTH_SHORT).show();
    }

}
