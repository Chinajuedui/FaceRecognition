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
import android.widget.TextView;
import android.widget.Toast;

import com.absolute.facerecognition.GetPhotoFromPhotoAlbum;
import com.absolute.facerecognition.R;
import com.bumptech.glide.Glide;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
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
 * 人脸对比Fragment(两张图片对比相似度)
 */
public class FaceComparsionFragment extends Fragment implements View.OnClickListener, EasyPermissions.PermissionCallbacks{

    ImageView comparse_iv_one;
    ImageView comparse_iv_two;
    Button comparse_btn_camera1;
    Button comparse_btn_photo1;
    Button comparse_btn_camera2;
    Button comparse_btn_photo2;
    Button comparse_btn_comparse;
    TextView comparse_tv_score;
    private View view;
    private ProgressDialog dialog;

    private File cameraSavePath1, cameraSavePath2;
    private Uri uri;

    // 相机权限和SD卡写入权限
    private final String[] PERMS = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.INTERNET};
    // 初始权限请求码
    private final int INIT_REQUESTCODE = 2000;
    // 相机相册请求码
    private final int REQUEST_CAMERA_CODE1 = 1;
    private final int REQUEST_ALBUM_CODE1 = 2;
    private final int REQUEST_CAMERA_CODE2 = 3;
    private final int REQUEST_ALBUM_CODE2 = 4;

    private final String COMPARSE_FACE_URL = "http://192.168.1.106:3030/match";


    public FaceComparsionFragment() {

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (view == null) {
            view = inflater.inflate(R.layout.fragment_face_comparsion, container, false);
        }
        initView();
        initPermissions();
        return view;
    }

    private void initView() {
        comparse_iv_one = view.findViewById(R.id.comparse_iv_one);
        comparse_iv_two = view.findViewById(R.id.comparse_iv_two);
        comparse_btn_camera1 = view.findViewById(R.id.comparse_btn_camera1);
        comparse_btn_photo1 = view.findViewById(R.id.comparse_btn_photo1);
        comparse_btn_camera2 = view.findViewById(R.id.comparse_btn_camera2);
        comparse_btn_photo2 = view.findViewById(R.id.comparse_btn_photo2);
        comparse_btn_comparse = view.findViewById(R.id.comparse_btn_comparse);
        comparse_tv_score = view.findViewById(R.id.comparse_tv_score);
        comparse_btn_camera1.setOnClickListener(this);
        comparse_btn_photo1.setOnClickListener(this);
        comparse_btn_camera2.setOnClickListener(this);
        comparse_btn_photo2.setOnClickListener(this);
        comparse_btn_comparse.setOnClickListener(this);
    }

    private void initPermissions(){
        if(!EasyPermissions.hasPermissions(getContext(), PERMS)) {
            // 没有权限,进行申请权限
            EasyPermissions.requestPermissions(new PermissionRequest.Builder(this, INIT_REQUESTCODE, PERMS).build());
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.comparse_btn_camera1:
                // 判断是否有相机权限
                if (EasyPermissions.hasPermissions(getContext(), new String[] {PERMS[0]})) {
                    //调用相机
                    startCamera(1);
                } else {
                    initPermissions();
                }
                break;
            case R.id.comparse_btn_photo1:
                // 判断是否有相册权限
                if (EasyPermissions.hasPermissions(getContext(), new String[] {PERMS[1]})) {
                    getAlbum(1);
                } else {
                    initPermissions();
                }
                break;
            case R.id.comparse_btn_camera2:
                // 判断是否有相机权限
                if (EasyPermissions.hasPermissions(getContext(), new String[] {PERMS[0]})) {
                    //调用相机
                    startCamera(2);
                } else {
                    initPermissions();
                }
                break;
            case R.id.comparse_btn_photo2:
                // 判断是否有相册权限
                if (EasyPermissions.hasPermissions(getContext(), new String[] {PERMS[1]})) {
                    getAlbum(2);
                } else {
                    initPermissions();
                }
                break;
            case R.id.comparse_btn_comparse:
                // 判断是否有联网权限
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

    private void startCamera(int flag) {
        if (flag == 1) {
            cameraSavePath1 = new File(Environment.getExternalStorageDirectory().getPath() + "/" + System.currentTimeMillis() + ".jpg");
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                //第二个参数为 包名.fileprovider
                uri = FileProvider.getUriForFile(getContext(), "com.absolute.fileprovider", cameraSavePath1);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else {
                uri = Uri.fromFile(cameraSavePath1);
            }
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
            startActivityForResult(intent, REQUEST_CAMERA_CODE1);
        } else {
            cameraSavePath2 = new File(Environment.getExternalStorageDirectory().getPath() + "/" + System.currentTimeMillis() + ".jpg");
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                //第二个参数为 包名.fileprovider
                uri = FileProvider.getUriForFile(getContext(), "com.absolute.fileprovider", cameraSavePath2);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else {
                uri = Uri.fromFile(cameraSavePath2);
            }
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
            startActivityForResult(intent, REQUEST_CAMERA_CODE2);
        }
    }

    private void getAlbum(int flag) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_PICK);
        intent.setType("image/*");
        if (flag == 1){
            startActivityForResult(intent, REQUEST_ALBUM_CODE1);
        } else {
            startActivityForResult(intent, REQUEST_ALBUM_CODE2);
        }

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        String photoPath;
        if (requestCode == REQUEST_CAMERA_CODE1 && resultCode == RESULT_OK) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                photoPath = String.valueOf(cameraSavePath1);
            } else {
                photoPath = uri.getEncodedPath();
            }
            Glide.with(this).load(photoPath).into(comparse_iv_one);
        } else if (requestCode == REQUEST_CAMERA_CODE2 && resultCode == RESULT_OK) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                photoPath = String.valueOf(cameraSavePath2);
            } else {
                photoPath = uri.getEncodedPath();
            }
            Glide.with(this).load(photoPath).into(comparse_iv_two);
        }else if (requestCode == REQUEST_ALBUM_CODE1 && resultCode == RESULT_OK) {
            photoPath = GetPhotoFromPhotoAlbum.getRealPathFromUri(getContext(), data.getData());
            cameraSavePath1 = new File(photoPath);
            Glide.with(getContext()).load(photoPath).into(comparse_iv_one);
        } else if (requestCode == REQUEST_ALBUM_CODE2&& resultCode == RESULT_OK) {
            photoPath = GetPhotoFromPhotoAlbum.getRealPathFromUri(getContext(), data.getData());
            cameraSavePath2 = new File(photoPath);
            Glide.with(getContext()).load(photoPath).into(comparse_iv_two);
        }
    }

    private void uploadImage() {
        if (cameraSavePath1 == null || !cameraSavePath1.exists()) {
            Toast.makeText(getContext(), "请先选择人脸识别图片1", Toast.LENGTH_SHORT).show();
        } else if (cameraSavePath2 == null || !cameraSavePath2.exists()){
            Toast.makeText(getContext(), "请先选择人脸识别图片2", Toast.LENGTH_SHORT).show();
        }else {
            dialog = new ProgressDialog(getContext());
            dialog.setTitle("对比中...");
            dialog.show();
            OkHttpClient client = new OkHttpClient.Builder()
                    .build();
            // 设置文件以及文件上传类型封装
            RequestBody requestBody1 = RequestBody.create(MediaType.parse("image/jpg"), cameraSavePath1);
            RequestBody requestBody2 = RequestBody.create(MediaType.parse("image/jpg"), cameraSavePath2);
            // 文件上传的请求体封装
            MultipartBody multipartBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("key", "csh")
                    .addFormDataPart("file0", cameraSavePath1.getName(), requestBody1)
                    .addFormDataPart("file1", cameraSavePath2.getName(), requestBody2)
                    .build();
            Request request = new Request.Builder()
                    .url(COMPARSE_FACE_URL)
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
                    Log.e("TAG", "SUCCESS!      " + responseString);
                    try {
                        JSONObject responseJson = new JSONObject(responseString);
                        final String errorMsg = responseJson.getString("error_msg");
                        if (errorMsg != null && errorMsg.equals("SUCCESS")) {
                            String result = responseJson.getString("result");
                            JSONObject resultJson = new JSONObject(result);
                            final double score = Double.valueOf(resultJson.getString("score"));
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    comparse_tv_score.setVisibility(View.VISIBLE);
                                    comparse_tv_score.setText("相似度:" + score);
                                }
                            });
                        } else {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(getContext(), "人脸对比失败" + errorMsg, Toast.LENGTH_SHORT).show();
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
