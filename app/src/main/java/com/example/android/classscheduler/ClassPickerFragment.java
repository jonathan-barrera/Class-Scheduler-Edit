package com.example.android.classscheduler;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SearchView;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by jonathanbarrera on 6/13/18.
 * Popup fragment that allows users to choose classes to add to a student's class list
 */

public class ClassPickerFragment extends android.support.v4.app.DialogFragment {

    // Keys
    public static final String PICKER_KEY = "picker";
    public static final String REMOVER_KEY = "remover";

    // Views
    @BindView(R.id.class_picker_search_view)
    SearchView mClassPickerSearchView;
    @BindView(R.id.class_picker_list_view)
    ListView mClassPickerListView;
    @BindView(R.id.class_picker_dismiss_button)
    Button mClassPickerDismissButton;
    @BindView(R.id.class_picker_create_class_button)
    Button mCreateClassButton;

    // Member variables
    private ArrayAdapter<String> mAdapter;
    onItemClickListener mCallback;

    public interface onItemClickListener {
        void onTitleSelected(String string, String key);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mCallback = (onItemClickListener) context;
    }

    // Empty Constructor
    public ClassPickerFragment() {}

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        // Inflate view
        View rootView = inflater.inflate(R.layout.fragment_class_picker, container, false);

        // Bind views
        ButterKnife.bind(this, rootView);

        // Declare List variables
        List<String> fullClassList;
        List<String> chosenClassList;
        final String pickerOrRemoverKey;

        // Initialize list (depending on whether we are adding classes or removing classes)
        if (getArguments().containsKey(EditStudentInfo.FULL_CLASS_LIST_KEY)) {
            fullClassList = getArguments().getStringArrayList(EditStudentInfo.FULL_CLASS_LIST_KEY);
            mAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, fullClassList);
            pickerOrRemoverKey = PICKER_KEY;
        }
        else {
            // Chosen Class List
            chosenClassList = getArguments().getStringArrayList(EditStudentInfo.CHOSEN_CLASS_LIST_KEY);
            mAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, chosenClassList);
            pickerOrRemoverKey = REMOVER_KEY;
        }

        // Create and set the adapter
        mClassPickerListView.setAdapter(mAdapter);

        // Set query hint in search view
        mClassPickerSearchView.setQueryHint(getString(R.string.search_class));

        mClassPickerListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (pickerOrRemoverKey.equals(PICKER_KEY)) {
                    String selectedClassTitle = mClassPickerListView.getItemAtPosition(position).toString();
                    mCallback.onTitleSelected(selectedClassTitle, pickerOrRemoverKey);
                } else if (pickerOrRemoverKey.equals(REMOVER_KEY)) {
                    String removedClassTitle = mClassPickerListView.getItemAtPosition(position).toString();
                    mCallback.onTitleSelected(removedClassTitle, pickerOrRemoverKey);
                }
                dismiss();
            }
        });

        // Set the OnQueryTextListener
        mClassPickerSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                mAdapter.getFilter().filter(newText);
                return true;
            }
        });


        // Set OnClickLiners for the two Buttons
        mClassPickerDismissButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        mCreateClassButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
                Intent intent = new Intent(getActivity(), CreateClassesActivity.class);
                startActivity(intent);
            }
        });

        return rootView;
    }
}
