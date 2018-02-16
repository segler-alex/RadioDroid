package net.programmierecke.radiodroid2.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.programmierecke.radiodroid2.data.DataCategory;
import net.programmierecke.radiodroid2.R;

import java.util.List;

public class ItemAdapterCategory extends RecyclerView.Adapter<ItemAdapterCategory.CategoryViewHolder> {

    public interface CategoryClickListener {
        void onCategoryClick(DataCategory category);
    }

    private List<DataCategory> categoriesList;
    private int resourceId;
    private CategoryClickListener categoryClickListener;

    class CategoryViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView textViewName;
        TextView textViewCount;

        CategoryViewHolder(View itemView) {
            super(itemView);
            textViewName = (TextView) itemView.findViewById(R.id.textViewTop);
            textViewCount = (TextView) itemView.findViewById(R.id.textViewBottom);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (categoryClickListener != null) {
                categoryClickListener.onCategoryClick(categoriesList.get(getAdapterPosition()));
            }
        }
    }

    public ItemAdapterCategory(int resourceId) {
        this.resourceId = resourceId;
    }

    public void setCategoryClickListener(CategoryClickListener categoryClickListener) {
        this.categoryClickListener = categoryClickListener;
    }

    public void updateList(List<DataCategory> categoriesList) {
        this.categoriesList = categoriesList;

        notifyDataSetChanged();
    }

    @Override
    public CategoryViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View v = inflater.inflate(resourceId, parent, false);

        return new CategoryViewHolder(v);
    }

    @Override
    public void onBindViewHolder(CategoryViewHolder holder, int position) {
        final DataCategory category = categoriesList.get(position);

        holder.textViewName.setText(category.Name);
        holder.textViewCount.setText(String.valueOf(category.UsedCount));
    }

    @Override
    public int getItemCount() {
        return categoriesList.size();
    }
}
