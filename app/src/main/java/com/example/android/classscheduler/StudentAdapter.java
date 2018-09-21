package com.example.android.classscheduler;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.android.classscheduler.model.Student;
import com.example.android.classscheduler.utils.DateUtils;

import org.apache.commons.text.WordUtils;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.example.android.classscheduler.EditStudentInfo.SEX_MALE_INT;

/**
 * Created by jonathanbarrera on 6/18/18.
 * Recycler view adapter for Student objects
 */

public class StudentAdapter extends RecyclerView.Adapter<StudentAdapter.ViewHolder> {

    // Constants
    public static final String STUDENT_ID_EXTRA_KEY = "student-id-extra";

    // Member variables
    private List<Student> mStudentData;

    @NonNull
    @Override
    public StudentAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.student_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final StudentAdapter.ViewHolder holder, int position) {
        // Extract all of the information for the current student
        final Student currentStudent = mStudentData.get(position);
        String studentName = WordUtils.capitalizeFully(currentStudent.getName());
        int studentSex = currentStudent.getSex();
        long studentBirthdate = currentStudent.getBirthdate();
        String studentPictureUrl = currentStudent.getPhotoUrl();

        String studentSexString;
        if (studentSex == SEX_MALE_INT) {
            studentSexString = holder.itemView.getContext().getResources().getString(R.string.male);
        } else {
            studentSexString = holder.itemView.getContext().getResources().getString(R.string.female);
        }

        // Populate the textviews with the data
        holder.studentNameTV.setText(studentName);
        holder.studentSexTV.setText(studentSexString);
        holder.studentAgeTV.setText(DateUtils.getAge(studentBirthdate));

        // Put a student picture if it exist
        if (!TextUtils.isEmpty(studentPictureUrl)) {
            Glide.with(holder.studentPictureIV.getContext())
                    .load(studentPictureUrl)
                    .into(holder.studentPictureIV);
        } else {
            holder.studentPictureIV.setImageResource(R.drawable.ic_baseline_account_circle_24px);
        }

        // Set onclicklistener
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Open the student profile for the student that was clicked on
                openStudentProfile(currentStudent, holder);
            }
        });
    }

    // Helper method for opening student profile for student clicked on
    private void openStudentProfile(Student student, ViewHolder holder) {
        // Use intent to open the StudentLocalDatabase Profile activity
        Intent intent = new Intent(holder.studentAgeTV.getContext(), StudentProfile.class);
        intent.putExtra(STUDENT_ID_EXTRA_KEY, student.getStudentId());
        holder.studentNameTV.getContext().startActivity(intent);
    }

    @Override
    public int getItemCount() {
        if (mStudentData == null) {
            return 0;
        } else {
            return mStudentData.size();
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder{

        @BindView(R.id.student_name_text_view) TextView studentNameTV;
        @BindView(R.id.student_sex_text_view) TextView studentSexTV;
        @BindView(R.id.student_age_text_view) TextView studentAgeTV;
        @BindView(R.id.student_picture_image_view)
        ImageView studentPictureIV;

        public ViewHolder(View itemView) {
            super(itemView);

            ButterKnife.bind(this, itemView);
        }
    }

    // Method for setting the Class data
    public void setClassData(List<Student> studentList) {
        mStudentData = studentList;
        notifyDataSetChanged();
    }
}
