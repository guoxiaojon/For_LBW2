package org.snowcorp.imageupload;

import android.Manifest;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.error.VolleyError;
import com.android.volley.request.SimpleMultiPartRequest;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private ImageView imageView;
    private TextView resultTV;
    private Button btnChoose, btnUpload;
    private Button btn_take_photo2;
    private ProgressBar progressBar;
    private static final int TAKE_PHOTO_REQUEST_TWO = 444;

    public static String BASE_URL = "http://159.226.21.115/caption/index.php";
    static final int PICK_IMAGE_REQUEST = 1;
    String filePath;
    Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imageView = (ImageView) findViewById(R.id.imageView);
        btnChoose = (Button) findViewById(R.id.button_choose);
        btnUpload = (Button) findViewById(R.id.button_upload);
        btn_take_photo2 = (Button) findViewById(R.id.button_take_photo);
        resultTV = (TextView)findViewById(R.id.result);
        btnChoose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                imageBrowse();
            }
        });

        btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Bitmap bm = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
                if (bm != null) {

                    filePath = compressBitmap(bm);

                    imageUpload(filePath);
                } else {
                    Toast.makeText(getApplicationContext(), "Image not selected!",
                        Toast.LENGTH_LONG).show();
                }
            }
        });

        btn_take_photo2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                //                startActivityForResult(intent, TAKE_PHOTO_REQUEST_TWO);

                SimpleDateFormat timeStampFormat = new SimpleDateFormat("yyyyMMddHHmmss");
                String filename = timeStampFormat.format(new Date());
                ContentValues values = new ContentValues(); //使用本地相册保存拍摄照片
                values.put(MediaStore.Images.Media.TITLE, filename);
                imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    values);

                // 设置 intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri); photoUri = 保存图片得uri
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                startActivityForResult(intent, TAKE_PHOTO_REQUEST_TWO);
            }
        });

        if (Build.VERSION.SDK_INT >= 23) {
            int REQUEST_CODE_CONTACT = 101;
            String[] permissions = { Manifest.permission.WRITE_EXTERNAL_STORAGE };
            //验证是否许可权限
            for (String str : permissions) {
                if (this.checkSelfPermission(str) != PackageManager.PERMISSION_GRANTED) {
                    //申请权限
                    this.requestPermissions(permissions, REQUEST_CODE_CONTACT);
                    return;
                }
            }
        }
    }

    private void imageBrowse() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK,
            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        // Start the Intent
        startActivityForResult(galleryIntent, PICK_IMAGE_REQUEST);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {

            if (requestCode == PICK_IMAGE_REQUEST) {
                Uri picUri = data.getData();
                filePath = getPath(picUri);

                Log.d("picUri", picUri.toString());
                Log.d("filePath", filePath);
                imageView.setImageBitmap(
                    decodeSampledBitmapFromFilePath(filePath, imageView.getWidth(),
                        imageView.getHeight()));
                //imageView.setImageURI(picUri);
            } else if (requestCode == TAKE_PHOTO_REQUEST_TWO) {

                Uri picUri = imageUri;
                filePath = getPath(imageUri);
                Log.d("picUri", picUri.toString());
                Log.d("filePath", filePath);
                imageView.setImageBitmap(
                    decodeSampledBitmapFromFilePath(filePath, imageView.getWidth(),
                        imageView.getHeight()));

                //imageView.setImageURI(picUri);
            }
        }

        if (resultCode == RESULT_CANCELED) {
            Toast.makeText(MainActivity.this, "点击取消", Toast.LENGTH_LONG).show();
            return;
        }
    }

    private void imageUpload(final String imagePath) {

        SimpleMultiPartRequest smr = new SimpleMultiPartRequest(Request.Method.POST, BASE_URL,
            new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    Log.d("Response", response);
                    resultTV.setText(response);
                    try {
                        JSONObject jObj = new JSONObject(response);
                        String message = jObj.getString("message");


                        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
                    } catch (JSONException e) {
                        // JSON error
                        e.printStackTrace();
                        Toast.makeText(getApplicationContext(), "Json error: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    }
                }
            }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                resultTV.setText("网络请求失败");
                Toast.makeText(getApplicationContext(), error.getMessage(), Toast.LENGTH_LONG)
                    .show();
            }
        });

        smr.addFile("image", imagePath);
        MyApplication.getInstance().addToRequestQueue(smr);
    }

    private String getPath(Uri contentUri) {
        String[] proj = { MediaStore.Images.Media.DATA };
        CursorLoader loader =
            new CursorLoader(getApplicationContext(), contentUri, proj, null, null, null);
        Cursor cursor = loader.loadInBackground();
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        String result = cursor.getString(column_index);
        cursor.close();
        return result;
    }

    /**
     * 压缩图片，返回路径
     */
    public String compressBitmap(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int options = 100;
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);

        while (baos.toByteArray().length / 1024 > 1024) {
            Log.d(TAG, " compress once");
            baos.reset();
            options -= 10;
            if (options < 11) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, options, baos);
                break;
            }
            bitmap.compress(Bitmap.CompressFormat.JPEG, options, baos);
        }

        String mDir = Environment.getExternalStorageDirectory() + "/LBW";
        File dir = new File(mDir);
        if (!dir.exists()) {
            dir.mkdirs();//文件不存在，则创建文件
        }
        String fileName = System.currentTimeMillis() + "_LBW_";
        File file = new File(mDir, fileName + ".jpg");

        try {
            FileOutputStream out = new FileOutputStream(file);
            out.write(baos.toByteArray());
            out.flush();
            out.close();
            int size = baos.toByteArray().length / 1024;
            Log.d(TAG, "compress image success" + ", size=" + size + " KB " );
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, "compress image fail");
        }

        return file.getAbsolutePath();
    }

    public static Bitmap decodeSampledBitmapFromFilePath(String imagePath, int reqWidth,
        int reqHeight) {
        // 第一次解析将inJustDecodeBounds设置为true，来获取图片大小
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imagePath, options);
        // 调用上面定义的方法计算inSampleSize值
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        // 使用获取到的inSampleSize值再次解析图片
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(imagePath, options);
    }

    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth,
        int reqHeight) {
        // 源图片的高度和宽度
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            // 计算出实际宽高和目标宽高的比率
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);
            // 选择宽和高中最小的比率作为inSampleSize的值，这样可以保证最终图片的宽和高
            // 一定都会大于等于目标的宽和高。
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }
        return inSampleSize;
    }
}
