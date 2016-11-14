package com.neek.tech.weatherapp.weatherama.ui.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;

import com.neek.tech.permissions.runtime_permission.PermissionConstants;
import com.neek.tech.permissions.runtime_permission.PermissionUtils;
import com.neek.tech.weatherapp.R;
import com.neek.tech.weatherapp.weatherama.base.BaseFragment;
import com.neek.tech.weatherapp.weatherama.controllers.WeatherController;
import com.neek.tech.weatherapp.weatherama.utilities.Logger;
import com.neek.tech.weatherapp.weatherama.utilities.WeatheramaDialog;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Settings fragment
 */
public class SettingsFragment extends BaseFragment implements
        PermissionUtils.PermissionListener {

    private static final String TAG = SettingsFragment.class.getSimpleName();

    @BindView(R.id.enableLocationButton)
    Button mEnableLocationButton;

    @BindView(R.id.listView)
    ListView mListView;

    private List<String> mAddressList;

    private SettingsAdapter mAdapter;

    private String mSelectedAddress;

    private SparseBooleanArray mCheckedItems;


    public static SettingsFragment newInstance() {
        return new SettingsFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Logger.create(TAG);
        mAddressList = WeatherController.getSavedUserLocations(getActivity());
    }

    @Override
    protected int onCreateViewId() {
        return R.layout.fragment_settings;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setLocationButtonState();

        initializeAdapter();

    }

    @Override
    public void onResume() {
        super.onResume();

        mSelectedAddress = WeatherController.getSelectedAddress(getActivity());
        Logger.i(TAG, "Selected address " + mSelectedAddress);
        setLocationButtonState();
        initializeAdapter();

    }

    @OnClick(value = R.id.enableLocationButton)
    public void onEnableLocationClicked() {
        PermissionUtils.with(this).requestPermission(PermissionConstants.LOCATION_PERMISSION);
    }

    private void setLocationButtonState() {
        if (!PermissionUtils.hasPermission(getActivity(), PermissionConstants.LOCATION_PERMISSION) &&
                PermissionUtils.isAndroidOSMarshmallowOrAbove()) {

            if (mEnableLocationButton != null && mEnableLocationButton.getVisibility() == View.GONE) {
                Animation fadeIn = AnimationUtils.loadAnimation(getActivity(), R.anim.fade_in);
                mEnableLocationButton.setVisibility(View.VISIBLE);
                mEnableLocationButton.startAnimation(fadeIn);

            }

        } else {

            if (mEnableLocationButton != null && mEnableLocationButton.getVisibility() == View.VISIBLE) {
                Animation fadeOut = AnimationUtils.loadAnimation(getActivity(), R.anim.fade_out);
                mEnableLocationButton.startAnimation(fadeOut);
                mEnableLocationButton.setVisibility(View.GONE);
            }
        }
    }

    private void initializeAdapter() {
        if (mAdapter == null) {
            mAdapter = new SettingsAdapter(getActivity(), mAddressList);
        }

        mCheckedItems.clear();
        mListView.setAdapter(mAdapter);
    }

    private void refreshAdapter() {
        mSelectedAddress = WeatherController.getSelectedAddress(getActivity());
        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public String getFragmentTag() {
        return TAG;
    }

    @Override
    public void onPermissionGranted(String permission) {
        setLocationButtonState();
    }

    @Override
    public void onPermissionDenied(String permission, boolean permanentDenial) {
        setLocationButtonState();
    }

    @Override
    public void onRationaleRequested(String permission) {
        Log.i(TAG, "Rationale requested for permission = " + permission);
        PermissionUtils.with(this).continueRequest();
    }

    class SettingsAdapter extends BaseAdapter {

        private List<String> addresses;
        private Context mContext;


        SettingsAdapter(Context context, List<String> addresses) {
            this.mContext = context;
            this.addresses = addresses;
            mCheckedItems = new SparseBooleanArray();
        }

        @Override
        public int getCount() {
            return (addresses != null) ? addresses.size() : 0;
        }

        @Override
        public Object getItem(int position) {
            return addresses.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            final SettingsItemViewHolder vh;

            if (convertView == null) {
                convertView = inflater.inflate(R.layout.item_settings, parent, false);
                vh = new SettingsItemViewHolder(convertView);
                convertView.setTag(vh);
            } else {
                vh = (SettingsItemViewHolder) convertView.getTag();
            }

            if (addresses != null && addresses.size() > 0) {
                final String address = addresses.get(position);
                if (!TextUtils.isEmpty(address) && vh.addressLabel != null) {
                    vh.addressLabel.setText(address);
                }

                if (vh.addressCheckbox != null) {

                    if (!TextUtils.isEmpty(mSelectedAddress) &&
                            mSelectedAddress.equalsIgnoreCase(address)) {
                        mCheckedItems.put(position, true);
                        if (mCheckedItems != null) {
                            vh.addressCheckbox.setChecked(mCheckedItems.get(position));
                        }
                    }

                    vh.addressCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                            boolean enabled = mCheckedItems.get(position);
                            if (!address.equalsIgnoreCase(mSelectedAddress) && !enabled && isChecked){
                                showUseAddressConfirmationDialog(mContext, address, position);
                            }

                            if (enabled && !isChecked){
                                showRemoveAddressConfirmationDialog(mContext, position);
                            }
                        }
                    });
                }
            }

            return convertView;
        }


        class SettingsItemViewHolder {

            @BindView(R.id.addressLabel)
            TextView addressLabel;

            @BindView(R.id.addressCheckbox)
            CheckBox addressCheckbox;

            SettingsItemViewHolder(View v) {
                ButterKnife.bind(this, v);
            }
        }
    }

    private void showUseAddressConfirmationDialog(final Context context, final String address, final int position) {
        WeatheramaDialog dialog = WeatheramaDialog.newInstance(null, getString(R.string.set_address_message),
                getString(R.string.yes), getString(R.string.no),
                new WeatheramaDialog.DialogClickedButton() {
                    @Override
                    public void onClickedButton(WeatheramaDialog dialog, int id, View v) {
                        if (id == WeatheramaDialog.POSITIVE_BUTTON) {

                            WeatherController.setSelectedAddress(context, address);

                            if (mCheckedItems != null) {
                                mCheckedItems.put(position, true);

                            }

                        } else if (id == WeatheramaDialog.NEGATIVE_BUTTON) {
                            //Do nothing
                            if (mCheckedItems != null) {
                                mCheckedItems.put(position, false);

                            }
                        }
                        refreshAdapter();

                    }
                });
        dialog.setCancelable(false);
        dialog.show(getFragmentManager(), WeatheramaDialog.TAG);
    }


    private void showRemoveAddressConfirmationDialog(final Context context, final int position) {
        WeatheramaDialog dialog = WeatheramaDialog.newInstance(null, getString(R.string.set_address_message),
                getString(R.string.remove), getString(R.string.cancel),
                new WeatheramaDialog.DialogClickedButton() {
                    @Override
                    public void onClickedButton(WeatheramaDialog dialog, int id, View v) {
                        if (id == WeatheramaDialog.POSITIVE_BUTTON) {

                            WeatherController.setSelectedAddress(context, null);

                            if (mCheckedItems != null) {
                                mCheckedItems.put(position, false);
                            }

                        } else if (id == WeatheramaDialog.NEGATIVE_BUTTON) {
                            //Do nothing
                            if (mCheckedItems != null) {
                                mCheckedItems.put(position, true);
                            }
                        }
                        refreshAdapter();


                    }
                });
        dialog.setCancelable(false);
        dialog.show(getFragmentManager(), WeatheramaDialog.TAG);
    }


}