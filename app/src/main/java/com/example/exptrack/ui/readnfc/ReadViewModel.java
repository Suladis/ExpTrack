package com.example.exptrack.ui.readnfc;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class ReadViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public ReadViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is the Read NFC View Model");
    }

    public LiveData<String> getText() {
        return mText;
    }
}
