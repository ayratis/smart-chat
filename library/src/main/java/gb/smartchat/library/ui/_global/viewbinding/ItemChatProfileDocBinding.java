// Generated by view binder compiler. Do not edit!
package gb.smartchat.library.ui._global.viewbinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import gb.smartchat.R;

public final class ItemChatProfileDocBinding implements ViewBinding {
  @NonNull
  private final ConstraintLayout rootView;

  @NonNull
  public final View divider;

  @NonNull
  public final ImageView ivIcon;

  @NonNull
  public final ProgressBar progressBarFile;

  @NonNull
  public final TextView tvName;

  @NonNull
  public final TextView tvSizeAndDate;

  private ItemChatProfileDocBinding(@NonNull ConstraintLayout rootView, @NonNull View divider,
      @NonNull ImageView ivIcon, @NonNull ProgressBar progressBarFile, @NonNull TextView tvName,
      @NonNull TextView tvSizeAndDate) {
    this.rootView = rootView;
    this.divider = divider;
    this.ivIcon = ivIcon;
    this.progressBarFile = progressBarFile;
    this.tvName = tvName;
    this.tvSizeAndDate = tvSizeAndDate;
  }

  @Override
  @NonNull
  public ConstraintLayout getRoot() {
    return rootView;
  }

  @NonNull
  public static ItemChatProfileDocBinding inflate(@NonNull LayoutInflater inflater) {
    return inflate(inflater, null, false);
  }

  @NonNull
  public static ItemChatProfileDocBinding inflate(@NonNull LayoutInflater inflater,
      @Nullable ViewGroup parent, boolean attachToParent) {
    View root = inflater.inflate(R.layout.item_chat_profile_doc, parent, false);
    if (attachToParent) {
      parent.addView(root);
    }
    return bind(root);
  }

  @NonNull
  public static ItemChatProfileDocBinding bind(@NonNull View rootView) {
    // The body of this method is generated in a way you would not otherwise write.
    // This is done to optimize the compiled bytecode for size and performance.
    int id;
    missingId: {
      id = R.id.divider;
      View divider = rootView.findViewById(id);
      if (divider == null) {
        break missingId;
      }

      id = R.id.iv_icon;
      ImageView ivIcon = rootView.findViewById(id);
      if (ivIcon == null) {
        break missingId;
      }

      id = R.id.progress_bar_file;
      ProgressBar progressBarFile = rootView.findViewById(id);
      if (progressBarFile == null) {
        break missingId;
      }

      id = R.id.tv_name;
      TextView tvName = rootView.findViewById(id);
      if (tvName == null) {
        break missingId;
      }

      id = R.id.tv_size_and_date;
      TextView tvSizeAndDate = rootView.findViewById(id);
      if (tvSizeAndDate == null) {
        break missingId;
      }

      return new ItemChatProfileDocBinding((ConstraintLayout) rootView, divider, ivIcon,
          progressBarFile, tvName, tvSizeAndDate);
    }
    String missingId = rootView.getResources().getResourceName(id);
    throw new NullPointerException("Missing required view with ID: ".concat(missingId));
  }
}
