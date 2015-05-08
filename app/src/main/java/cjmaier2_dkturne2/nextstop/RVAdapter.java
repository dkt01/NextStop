package cjmaier2_dkturne2.nextstop;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.shapes.Shape;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

public class RVAdapter extends RecyclerView.Adapter<RVAdapter.BusStopViewHolder> {

    public static class BusStopViewHolder extends RecyclerView.ViewHolder {

        CardView cv;
        TextView stopName;
        TextView stopDistance;
        LinearLayout stopRoutes;

        BusStopViewHolder(View itemView) {
            super(itemView);
            cv = (CardView)itemView.findViewById(R.id.cv);
            stopName = (TextView)itemView.findViewById(R.id.stop_name);
            stopDistance = (TextView)itemView.findViewById(R.id.stop_distance);
            stopRoutes = (LinearLayout)itemView.findViewById(R.id.stop_routes);
        }
    }

    List<BusStopData> busStops;
    Context ctx;

    RVAdapter(List<BusStopData> busStops){
        this.busStops = busStops;
    }

    RVAdapter(BusStopData busStop){
        this.busStops.add(busStop);
    }

    RVAdapter(){
        // Doesn't really need to do anything
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }

    @Override
    public BusStopViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        ctx = viewGroup.getContext();
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.busstop_card, viewGroup, false);
        BusStopViewHolder bsvh = new BusStopViewHolder(v);
        return bsvh;
    }

    @Override
    public void onBindViewHolder(BusStopViewHolder busStopViewHolder, int i) {
        List<Route> routes = busStops.get(i).routes;
        busStopViewHolder.stopName.setText(busStops.get(i).name);
        busStopViewHolder.stopDistance.setText(Integer.toString(busStops.get(i).distance) + " m");
        int vsize = 100;
        vsize /= routes.size()>0?routes.size():1;
        busStopViewHolder.stopRoutes.removeAllViews();
        for(Route route:routes) {
            Bitmap bitmap = Bitmap.createBitmap( dipToPixels(ctx,50), dipToPixels(ctx,vsize), Bitmap.Config.ARGB_8888 );
            bitmap.eraseColor(route.getColVal());
            ImageView rectim = new ImageView(ctx);
            rectim.setImageBitmap(bitmap);
            busStopViewHolder.stopRoutes.addView(rectim);
        }
    }

    @Override
    public int getItemCount() {
        return busStops.size();
    }

    public static int dipToPixels(Context context, int dipValue) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dipValue, metrics);
    }
}