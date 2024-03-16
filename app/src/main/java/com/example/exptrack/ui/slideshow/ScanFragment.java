package com.example.exptrack.ui.slideshow;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.exptrack.CaptureAct;
import com.example.exptrack.databinding.FragmentSlideshowBinding;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;



import com.example.exptrack.R;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class ScanFragment extends Fragment {

private FragmentSlideshowBinding binding;

    private Button b2,b3,b4,b6;

    private TextView tv2,tv4,tv6;

    private TableLayout tl1;

    private EditText et1;

    private ImageView iv1;

    public View onCreateView(@NonNull LayoutInflater inflater,
            ViewGroup container, Bundle savedInstanceState) {
        ScanViewModel slideshowViewModel =
                new ViewModelProvider(this).get(ScanViewModel.class);

    binding = FragmentSlideshowBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        b2 = root.findViewById(R.id.b2);
        b3 = root.findViewById(R.id.b3);
        b4 = root.findViewById(R.id.b4);
        tl1 = root.findViewById(R.id.tl1);
        tv2 = root.findViewById(R.id.tv2);
        tv4 = root.findViewById(R.id.tv4);
        tv6 = root.findViewById(R.id.tv6);
        et1 = root.findViewById(R.id.et1);
        b2.setOnClickListener(v -> {
            scanCode();
        });

        b3.setOnClickListener(v -> {

        });



// Change your onClickListener for saving data to the database
        b4.setOnClickListener(v -> {

        });

        return root;
    }

    private void scanCode() {
        ScanOptions options = new ScanOptions();
        options.setPrompt("Volume up to flash on");
        options.setBeepEnabled(true);
        options.setOrientationLocked(true);
        options.setCaptureActivity(CaptureAct.class);
        barLauncher.launch(options);
    }


    private ActivityResultLauncher<ScanOptions> barLauncher = registerForActivityResult(new ScanContract(), result -> {
        if (result.getContents() != null) {

            // Make Table Visible
            tl1.setVisibility(View.VISIBLE);

            // Extract UPC or EAN number from the barcode result
            String barcode = result.getContents();

            // Make HTTP request to UPC Database API
            OkHttpClient client = new OkHttpClient();
            HttpUrl.Builder urlBuilder = HttpUrl.parse("https://api.upcdatabase.org/product/" + barcode).newBuilder();
            urlBuilder.addQueryParameter("apikey", "806B6CBDB7002EED9D65ACBC355D74EA");
            String url = urlBuilder.build().toString();

            Request request = new Request.Builder()
                    .url(url)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        throw new IOException("Unexpected code " + response);
                    }

                    try {
                        // Parse the JSON response
                        JSONObject jsonResponse = new JSONObject(response.body().string());

                        // ID of Product
                        long productID = Long.parseLong(jsonResponse.optString("barcode"));

                        requireActivity().runOnUiThread(() -> {
                            tv2.setText(String.valueOf(productID));
                        });

                        // Alias Name of Product
                        String productName = jsonResponse.optString("title");
                        requireActivity().runOnUiThread(() -> {
                            tv4.setText(productName);
                        });

                        // Date Added
                        LocalDate localDate = LocalDate.now();
                        DateTimeFormatter formatDate = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                        String dateNow = localDate.format(formatDate);
                        requireActivity().runOnUiThread(() -> {
                            tv6.setText(dateNow);
                        });

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }


                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                }
            });
        }
    });


    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}