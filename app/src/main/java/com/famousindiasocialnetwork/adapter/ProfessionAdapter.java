package com.famousindiasocialnetwork.adapter;

import android.content.Context;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;

import com.famousindiasocialnetwork.R;

import java.util.List;

/**
 * Created by a_man on 17-02-2018.
 */

public class ProfessionAdapter extends RecyclerView.Adapter<ProfessionAdapter.MyViewHolder> {
    private final Context context;
    private final List<String> dataList;
    private ProfessionSelectedListener professionSelectedListener;

    private int selectedIndex = -1;

    public ProfessionAdapter(Context context, List<String> professions, int selectedIndex, ProfessionSelectedListener professionSelectedListener) {
        this.context = context;
        this.dataList = professions;
        this.selectedIndex = selectedIndex;
        this.professionSelectedListener = professionSelectedListener;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new MyViewHolder(LayoutInflater.from(context).inflate(R.layout.item_profession, parent, false));
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        holder.setData(dataList.get(position));
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        private RadioButton professionSelected;
        private TextView profession;

        public MyViewHolder(View itemView) {
            super(itemView);
            professionSelected = itemView.findViewById(R.id.professionSelected);
            profession = itemView.findViewById(R.id.profession);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    selectedIndex = getAdapterPosition();
                    professionSelectedListener.onProfessionSelected(dataList.get(selectedIndex));
                    notifyDataSetChanged();
                }
            });
        }

        public void setData(String pro) {
            profession.setText(pro);
            professionSelected.setChecked(getAdapterPosition() == selectedIndex);
        }
    }

    public interface ProfessionSelectedListener {
        void onProfessionSelected(String profession);
    }
}
