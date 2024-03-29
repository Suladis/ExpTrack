package com.example.exptrack.ui.writenfc;


import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class WriteViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public WriteViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is the Write NFC View Model");
    }

    public LiveData<String> getText() {
        return mText;
    }
}
