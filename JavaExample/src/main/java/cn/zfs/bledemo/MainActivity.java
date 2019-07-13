package cn.zfs.bledemo;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.snail.commons.entity.PermissionsRequester;
import com.snail.easyble.callback.ScanListener;
import com.snail.easyble.core.Ble;
import com.snail.easyble.core.Device;
import com.snail.easyble.core.ScanConfig;
import com.snail.widget.listview.BaseListAdapter;
import com.snail.widget.listview.BaseViewHolder;
import com.snail.widget.listview.PullRefreshLayout;
import com.tencent.mmkv.MMKV;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * date: 2019/5/31 17:11
 * author: zengfansheng
 */
public class MainActivity extends AppCompatActivity {
    private ListAdapter listAdapter;
    private PullRefreshLayout refreshLayout;
    private ListView lv;
    private LinearLayout layoutEmpty;
    private List<Device> devList = new ArrayList<>();

    private void assignViews() {
        refreshLayout = findViewById(R.id.refreshLayout);
        lv = findViewById(R.id.lv);
        layoutEmpty = findViewById(R.id.layoutEmpty);
    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initialize();
        initViews();
        Ble.Companion.getInstance().addScanListener(scanListener);
        ScanConfig scanConfig = new ScanConfig().setAcceptSysConnectedDevice(true)
                .setHideNonBleDevice(true)
                .setUseBluetoothLeScanner(MMKV.defaultMMKV().getBoolean(Consts.SP_KEY_USE_NEW_SCANNER, true));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            scanConfig.setScanSettings(new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build());
        }
        Ble.Companion.getInstance().getBleConfig().setScanConfig(scanConfig);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Ble.Companion.getInstance().release();
        System.exit(0);
    }

    private ScanListener scanListener = new ScanListener() {
        @Override
        public void onScanStart() {
            if (!refreshLayout.isRefreshing()) {
                refreshLayout.setRefreshing(true);
            }
        }

        @Override
        public void onScanStop() {
            refreshLayout.setRefreshing(false);
        }

        @Override
        public void onScanResult(@NotNull Device device) {
            refreshLayout.setRefreshing(false);
            layoutEmpty.setVisibility(View.INVISIBLE);
            listAdapter.add(device);
        }

        @Override
        public void onScanError(int errorCode, @NotNull String errorMsg) {

        }
    };

    private void initViews() {
        assignViews();
        refreshLayout.setColorSchemeColors(ContextCompat.getColor(this, R.color.colorAccent));
        listAdapter = new ListAdapter(this, devList);
        lv.setAdapter(listAdapter);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(MainActivity.this, ConnectionActivity.class);
                intent.putExtra(Consts.EXTRA_DEVICE, devList.get(position));
                startActivity(intent);
            }
        });
        refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (Ble.Companion.getInstance().isInitialized()) {
                    Ble.Companion.getInstance().stopScan();
                    doStartScan();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("Main", "onResume");
        if (Ble.Companion.getInstance().isInitialized()) {
            if (Ble.Companion.getInstance().isBluetoothAdapterEnabled()) {
                doStartScan();
            } else {
                startActivity(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (Ble.Companion.getInstance().isInitialized()) {
            Ble.Companion.getInstance().stopScan();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menuStop) {
            if (Ble.Companion.getInstance().isInitialized()) {
                refreshLayout.setRefreshing(false);
                Ble.Companion.getInstance().stopScan();
            }
        }
        invalidateOptionsMenu();
        return super.onOptionsItemSelected(item);
    }

    private void initialize() {
        //动态申请权限
        PermissionsRequester permissionsRequester = new PermissionsRequester(this);
        permissionsRequester.setOnRequestResultListener(new PermissionsRequester.OnRequestResultListener() {
            @Override
            public void onRequestResult(@NotNull List<String> list) {
                
            }
        });
        permissionsRequester.checkAndRequest(getNeedPermissions());
    }

    //需要进行检测的权限
    private List<String> getNeedPermissions() {
        List<String> list = new ArrayList<>();
        list.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        return list;
    }

    private void doStartScan() {
        listAdapter.clear();
        layoutEmpty.setVisibility(View.VISIBLE);
        Ble.Companion.getInstance().startScan();
        Log.d("Main", "doStartScan");
    }
    
    private class ListAdapter extends BaseListAdapter<Device> {
        private HashMap<String, Long> updateTimeMap = new HashMap<>();
        private HashMap<String, TextView> rssiViews = new HashMap<>();
        
        ListAdapter(@NotNull Context context, @NotNull List<Device> list) {
            super(context, list);
        }

        @NotNull
        @Override
        protected BaseViewHolder<Device> createViewHolder(int i) {            
            return new BaseViewHolder<Device>() {
                TextView tvName;
                TextView tvAddr;
                TextView tvRssi;
                
                @Override
                public void onBind(Device device, int i) {
                    rssiViews.put(device.getAddr(), tvRssi);
                    tvName.setText(device.getName());
                    tvAddr.setText(device.getAddr());
                    tvRssi.setText("" + device.getRssi());
                }

                @NotNull
                @Override
                public View createView() {
                    View view = View.inflate(getContext(), R.layout.item_scan, null);
                    tvName = view.findViewById(R.id.tvName);
                    tvAddr = view.findViewById(R.id.tvAddr);
                    tvRssi = view.findViewById(R.id.tvRssi);
                    return view;
                }
            };
        }
        
        void clear() {
            getItems().clear();
            notifyDataSetChanged();
        }
        
        void add(Device device) {
            Device dev = null;
            for (Device item : getItems()) {
                if (item.getAddr().equals(device.getAddr())) {
                    dev = item;
                    break;
                }
            }
            if (dev == null) {
                updateTimeMap.put(device.getAddr(), System.currentTimeMillis());
                getItems().add(device);
                notifyDataSetChanged();
            } else {
                Long time = updateTimeMap.get(device.getAddr());
                if (time == null || System.currentTimeMillis() - time > 2000) {
                    updateTimeMap.put(device.getAddr(), System.currentTimeMillis());
                    if (dev.getRssi() != device.getRssi()) {
                        dev.setRssi(device.getRssi());
                        final TextView tvRssi = rssiViews.get(device.getAddr());
                        tvRssi.setText("" + device.getRssi());
                        tvRssi.setTextColor(Color.BLACK);
                        tvRssi.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                tvRssi.setTextColor(0xFF909090);
                            }
                        }, 800);
                    }
                }
            }
        }
    }
}
