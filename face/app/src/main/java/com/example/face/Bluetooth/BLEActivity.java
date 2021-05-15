package com.example.face.Bluetooth;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.example.face.Bluetooth.view.PullRefreshListView;
import com.example.face.R;
import com.example.face.Bluetooth.view.PullToRefreshFrameLayout;
import com.inuker.bluetooth.library.BluetoothClient;
import com.inuker.bluetooth.library.connect.listener.BluetoothStateListener;
import com.inuker.bluetooth.library.search.SearchRequest;
import com.inuker.bluetooth.library.search.SearchResult;
import com.inuker.bluetooth.library.search.response.SearchResponse;
import com.inuker.bluetooth.library.utils.BluetoothLog;

import java.util.ArrayList;
import java.util.List;

public class BLEActivity extends Activity {

    private static final String MAC = "B0:D5:9D:6F:E7:A5";

    private PullToRefreshFrameLayout mRefreshLayout;
    private PullRefreshListView mListView;
    private DeviceListAdapter mAdapter;
    private TextView mTvTitle;

    private List<SearchResult> mDevices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ble_content_main);

        mDevices = new ArrayList<SearchResult>();

        mTvTitle = findViewById(R.id.title);

        mRefreshLayout = findViewById(R.id.pulllayout);

        mListView = mRefreshLayout.getPullToRefreshListView();
        mAdapter = new DeviceListAdapter(this);
        mListView.setAdapter(mAdapter);

        mListView.setOnRefreshListener(new PullRefreshListView.OnRefreshListener() {

            @Override
            public void onRefresh() {
                // TODO Auto-generated method stub
                searchDevice();
            }

        });

        if (Build.VERSION.SDK_INT >= 23) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 2);
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.READ_CONTACTS)) {
                    Toast.makeText(this, "shouldShowRequestPermissionRationale", Toast.LENGTH_SHORT).show();
                }
            }
        }

        searchDevice();

        ClientManager.getClient().registerBluetoothStateListener(new BluetoothStateListener() {
            @Override
            public void onBluetoothStateChanged(boolean openOrClosed) {
                BluetoothLog.v(String.format("onBluetoothStateChanged %b", openOrClosed));
            }
        });
    }

    private void searchDevice() {
        SearchRequest request = new SearchRequest.Builder()
                .searchBluetoothLeDevice(5000)
                .searchBluetoothClassicDevice(3000)
                .build();

        ClientManager.getClient().search(request, mSearchResponse);
    }

    private final SearchResponse mSearchResponse = new SearchResponse() {
        @Override
        public void onSearchStarted() {
            BluetoothLog.w("MainActivity.onSearchStarted");
            mListView.onRefreshComplete(true);
            mRefreshLayout.showState(AppConstants.LIST);
            mTvTitle.setText(R.string.string_refreshing);
            mDevices.clear();
        }

        @Override
        public void onDeviceFounded(SearchResult device) {
//            BluetoothLog.w("MainActivity.onDeviceFounded " + device.device.getAddress());
            if (!mDevices.contains(device)) {
                mDevices.add(device);
                mAdapter.setDataList(mDevices);

//                Beacon beacon = new Beacon(device.scanRecord);
//                BluetoothLog.v(String.format("beacon for %s\n%s", device.getAddress(), beacon.toString()));

//                BeaconItem beaconItem = null;
//                BeaconParser beaconParser = new BeaconParser(beaconItem);
//                int firstByte = beaconParser.readByte(); // 读取第1个字节
//                int secondByte = beaconParser.readByte(); // 读取第2个字节
//                int productId = beaconParser.readShort(); // 读取第3,4个字节
//                boolean bit1 = beaconParser.getBit(firstByte, 0); // 获取第1字节的第1bit
//                boolean bit2 = beaconParser.getBit(firstByte, 1); // 获取第1字节的第2bit
//                beaconParser.setPosition(0); // 将读取起点设置到第1字节处
            }

            if (mDevices.size() > 0) {
                mRefreshLayout.showState(AppConstants.LIST);
            }
        }

        @Override
        public void onSearchStopped() {
            BluetoothLog.w("MainActivity.onSearchStopped");
            mListView.onRefreshComplete(true);
            mRefreshLayout.showState(AppConstants.LIST);

            mTvTitle.setText(R.string.devices);
        }

        @Override
        public void onSearchCanceled() {
            BluetoothLog.w("MainActivity.onSearchCanceled");

            mListView.onRefreshComplete(true);
            mRefreshLayout.showState(AppConstants.LIST);

            mTvTitle.setText(R.string.devices);
        }
    };

    @Override
    protected void onPause() {
        super.onPause();
        ClientManager.getClient().stopSearch();
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 2) {
            if (permissions[0].equals(Manifest.permission.ACCESS_COARSE_LOCATION) && grantResults[0]
                    == PackageManager.PERMISSION_GRANTED) {
            } else {
                if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.
                        permission.ACCESS_COARSE_LOCATION)) {
                    return;
                }
            }
        }
    }
}
