package com.example.photoreminder;

import static com.example.photoreminder.MainActivity.SHARED_PREFERENCES_FILE_NAME;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.Toast;

import androidx.fragment.app.DialogFragment;

public class SetIntervalFragment extends DialogFragment {

    NumberPicker numberPickerHours;
    NumberPicker numberPickerMinutes;
    int MINIMUM_TIME_IN_MINUTES = 10;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {


        // Get the layout inflater
        LayoutInflater inflater = requireActivity().getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        View customView = inflater.inflate(R.layout.interval_picker, null);

        numberPickerHours = customView.findViewById(R.id.pickerHours);
        numberPickerMinutes = customView.findViewById(R.id.pickerMinutes);

        numberPickerHours.setMinValue(0);
        numberPickerHours.setMaxValue(100);

        numberPickerMinutes.setMinValue(0);
        numberPickerMinutes.setMaxValue(59);

        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage("Set time interval")
                .setView(customView)
                .setPositiveButton("Done", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        int hours = numberPickerHours.getValue();
                        int minutes = numberPickerMinutes.getValue();

                        if (hours<0 || minutes<0){
                            Toast.makeText(getContext(), "Hours and minutes must be positive values", Toast.LENGTH_SHORT).show();
                        }else if (hours==0 && minutes<MINIMUM_TIME_IN_MINUTES){
                            Toast.makeText(getContext(), "Please choose an interval of at least " + MINIMUM_TIME_IN_MINUTES + " minutes", Toast.LENGTH_SHORT).show();
                        }else {
                            SharedPreferences sharedPref = getContext().getSharedPreferences(SHARED_PREFERENCES_FILE_NAME,Context.MODE_PRIVATE);
                            sharedPref.edit().putInt("IntervalHours", hours)
                                    .putInt("IntervalMinutes", minutes).apply();
                        }

                        MainActivity mainActivity = ((MainActivity) requireActivity());
                        mainActivity.updateSetIntervalButton();
                        mainActivity.cancelJobAndConsiderStartingAgain();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                    }
                });
        // Create the AlertDialog object and return it
        return builder.create();
    }
}