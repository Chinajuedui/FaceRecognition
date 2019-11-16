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
 * 人脸检测Fragment.(年龄，颜值)
 */
public class FaceDetectFragment extends Fragment implements View.OnClickListener, EasyPermissions.PermissionCallbacks {

    ImageView main_iv_img;
    Button main_btn_camera;
    Button main_btn_photo;
    Button main_btn_detect;
    TextView main_tv_age;
    TextView main_tv_sex;
    TextView main_tv_facescore;
    private View view;
    private ProgressDialog dialog;

    private File cameraSavePath;
    private Uri uri;

    // 相机权限和SD卡写入权限
    private final String[] PERMS = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.INTERNET};
    // 初始权限请求码
    private final int INIT_REQUESTCODE = 2000;
    // 相机相册请求码
    private final int REQUEST_CAMERA_CODE = 1;
    private final int REQUEST_ALBUM_CODE = 2;

    private final String DETECT_FACE_URL = "http://192.168.1.106:3030/detect";


    public FaceDetectFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (view == null) {
            view = inflater.inflate(R.layout.fragment_face_detect, container, false);
        }
        initView();
        initPermissions();
        return view;
    }

    private void initPermissions(){
        if(!EasyPermissions.hasPermissions(getContext(), PERMS)) {
            // 没有权限,进行申请权限
            EasyPermissions.requestPermissions(new PermissionRequest.Builder(this, INIT_REQUESTCODE, PERMS).build());
        }
    }

    private void initView(){
        main_iv_img = view.findViewById(R.id.main_iv_img);
        main_btn_camera = view.findViewById(R.id.main_btn_camera);
        main_btn_photo = view.findViewById(R.id.main_btn_photo);
        main_btn_detect = view.findViewById(R.id.main_btn_detect);
        main_tv_age = view.findViewById(R.id.main_tv_age);
        main_tv_sex = view.findViewById(R.id.main_tv_sex);
        main_tv_facescore = view.findViewById(R.id.main_tv_facescore);
        main_btn_camera.setOnClickListener(this);
        main_btn_photo.setOnClickListener(this);
        main_btn_detect.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.main_btn_camera:
                // 判断是否有相机权限
                if (EasyPermissions.hasPermissions(getContext(), new String[] {PERMS[0]})) {
                    //调用相机
                    startCamera();
                } else {
                    initPermissions();
                }
                break;
            case R.id.main_btn_photo:
                // 判断是否有相册权限
                if (EasyPermissions.hasPermissions(getContext(), new String[] {PERMS[1]})) {
                    getAlbum();
                } else {
                    initPermissions();
                }
                break;
            case R.id.main_btn_detect:
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
            Glide.with(this).load(photoPath).into(main_iv_img);
        } else if (requestCode == REQUEST_ALBUM_CODE && resultCode == RESULT_OK) {
            photoPath = GetPhotoFromPhotoAlbum.getRealPathFromUri(getContext(), data.getData());
            cameraSavePath = new File(photoPath);
            Glide.with(getContext()).load(photoPath).into(main_iv_img);
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
            dialog.setTitle("检测中...");
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
            Request request = new Request.Builder()
                    .url(DETECT_FACE_URL)
                    .post(multipartBody)
                    .build();
            Call call = client.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(@NotNull Call call, @NotNull final IOException e) {
                    Log.e("TAG", "网络错误" + e.toString());
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
                    try {
                        JSONObject resultJson = new JSONObject(responseString);
                        String errorMsg = resultJson.getString("error_msg");
                        if (errorMsg.equals("SUCCESS")) {
                            Log.e("TAG", "SUCCESS");
                            String result = resultJson.getString("result");
                            JSONObject resultJ = new JSONObject(result);
                            String faceList = resultJ.getString("face_list");
                            faceList = faceList.replace("[", "");
                            faceList = faceList.replace("]", "");
                            JSONObject faceJson = new JSONObject(faceList);
                            final int age = faceJson.getInt("age");
                            final double beauty = faceJson.getDouble("beauty");
                            String gender = faceJson.getString("gender");
                            Log.e("TAG", age + " " + beauty + " " + gender);
                            JSONObject genderJson = new JSONObject(gender);
                            String type = genderJson.getString("type");
                            if (type != null && type.equals("male")) {
                                gender = "男性";
                            } else {
                                gender = "女性";
                            }
                            final String finalGender = gender;
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    main_tv_age.setVisibility(View.VISIBLE);
                                    main_tv_sex.setVisibility(View.VISIBLE);
                                    main_tv_facescore.setVisibility(View.VISIBLE);
                                    main_tv_age.setText("年龄:"+age);
                                    main_tv_sex.setText("性别:" + finalGender);
                                    main_tv_facescore.setText("颜值:" + beauty);
                                }
                            });
                        } else {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    main_tv_age.setVisibility(View.VISIBLE);
                                    main_tv_sex.setVisibility(View.VISIBLE);
                                    main_tv_facescore.setVisibility(View.VISIBLE);
                                    main_tv_age.setText("脸型:null");
                                    main_tv_sex.setText("性别:null");
                                    main_tv_facescore.setText("颜值:null");
                                }
                            });
                        }
                    } catch (JSONException e) {
                        Log.e("TAG", "" + e.toString());
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
