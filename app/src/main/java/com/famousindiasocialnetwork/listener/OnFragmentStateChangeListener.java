package com.famousindiasocialnetwork.listener;

/**
 * Denotes various states of a fragment
 */
public interface OnFragmentStateChangeListener {
    void onDetach();
    void onPause();
    void onOther(String i);
}
