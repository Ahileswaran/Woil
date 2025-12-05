package com.example.woil.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.woil.CategoryAdapter;
import com.example.woil.CategoryModel;
import com.example.woil.R;
import com.example.woil.TimelineAdapter;
import com.example.woil.TimelineModel;
import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        RecyclerView rvCategories = view.findViewById(R.id.rvCategories);
        RecyclerView rvTimeline = view.findViewById(R.id.rvTimeline);

        List<CategoryModel> categoryList = new ArrayList<>();
        categoryList.add(new CategoryModel("Plumbing", R.drawable.ic_plumber));
        categoryList.add(new CategoryModel("Electrician", R.drawable.ic_electrician));
        categoryList.add(new CategoryModel("Cleaning", R.drawable.ic_cleaning));

        rvCategories.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        CategoryAdapter catAdapter = new CategoryAdapter(categoryList, getContext());
        rvCategories.setAdapter(catAdapter);

        List<TimelineModel> timelineList = new ArrayList<>();
        timelineList.add(new TimelineModel("Fix sink leak", "Pending", "Today 2PM"));
        timelineList.add(new TimelineModel("Install ceiling fan", "Completed", "Yesterday"));
        timelineList.add(new TimelineModel("Paint living room", "Ongoing", "Tomorrow"));

        rvTimeline.setLayoutManager(new LinearLayoutManager(getContext()));
        TimelineAdapter tAdapter = new TimelineAdapter(timelineList);
        rvTimeline.setAdapter(tAdapter);

        return view;
    }
}