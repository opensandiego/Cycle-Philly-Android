package org.phillyopen.mytracks.cyclephilly;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.firebase.client.Firebase;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Created by toby on 7/20/15.
 */
public class RideIndegoAdapter extends RecyclerView.Adapter<RideIndegoAdapter.MyViewHolder>{

    private LayoutInflater inflater;
    private Firebase mRef;
    private List<IndegoStation> mstations = Collections.emptyList();
    private Context mContext;

    public RideIndegoAdapter(Context context,List<IndegoStation> stations){
        inflater=LayoutInflater.from(context);
        this.mstations = stations;
        this.mContext = context;
        System.out.println(mstations.size());
    }
    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view=inflater.inflate(R.layout.indego_list_row, viewGroup, false);
        MyViewHolder holder=new MyViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(MyViewHolder myViewHolder, int i) {
        final IndegoStation current=mstations.get(i);
        myViewHolder.name.setText(current.name);
        System.out.println(mstations.size());
    }

    @Override
    public int getItemCount() {
        return mstations.size();
    }

    public void removeAll(){
        mstations.removeAll(mstations);
        notifyAll();
    }

    class MyViewHolder extends RecyclerView.ViewHolder {

        TextView name;
        public MyViewHolder(View itemView) {
            super(itemView);
            name = (TextView) itemView.findViewById(R.id.stationName);
        }

        public void addView(IndegoStation model, int pos){

        }


    }
}
