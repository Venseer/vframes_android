package com.angarron.sfvframedata.ui.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.angarron.sfvframedata.R;
import com.angarron.sfvframedata.adapter.MovesRecyclerViewAdapter;

import java.util.List;
import java.util.Map;

import data.model.move.IMoveListMove;
import data.model.move.MoveCategory;

public class MoveListFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        RecyclerView moveListRecyclerView = (RecyclerView) inflater.inflate(R.layout.moves_list_recycler, container, false);
        setupRecyclerView(moveListRecyclerView);
        return moveListRecyclerView;
    }

    private void setupRecyclerView(RecyclerView recyclerView) {
        Activity hostActivity = getActivity();
        recyclerView.setLayoutManager(new LinearLayoutManager(hostActivity));

        IMoveListFragmentHost host = (IMoveListFragmentHost) hostActivity;
        recyclerView.setAdapter(new MovesRecyclerViewAdapter(getContext(), host.getMoveList()));
    }

    public interface IMoveListFragmentHost {
        Map<MoveCategory, List<IMoveListMove>> getMoveList();
    }

}
