// Generated by view binder compiler. Do not edit!
package gb.smartchat.library.ui._global.viewbinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import gb.smartchat.R;

public final class FragmentChatListBinding implements ViewBinding {
  @NonNull
  private final CoordinatorLayout rootView;

  @NonNull
  public final AppBarLayout appBarLayout;

  @NonNull
  public final FloatingActionButton btnCreateChat;

  @NonNull
  public final ImageView ivProfileAvatar;

  @NonNull
  public final LinearLayout profileContent;

  @NonNull
  public final RecyclerView rvChatList;

  @NonNull
  public final Toolbar toolbar;

  @NonNull
  public final TextView tvProfileName;

  private FragmentChatListBinding(@NonNull CoordinatorLayout rootView,
      @NonNull AppBarLayout appBarLayout, @NonNull FloatingActionButton btnCreateChat,
      @NonNull ImageView ivProfileAvatar, @NonNull LinearLayout profileContent,
      @NonNull RecyclerView rvChatList, @NonNull Toolbar toolbar, @NonNull TextView tvProfileName) {
    this.rootView = rootView;
    this.appBarLayout = appBarLayout;
    this.btnCreateChat = btnCreateChat;
    this.ivProfileAvatar = ivProfileAvatar;
    this.profileContent = profileContent;
    this.rvChatList = rvChatList;
    this.toolbar = toolbar;
    this.tvProfileName = tvProfileName;
  }

  @Override
  @NonNull
  public CoordinatorLayout getRoot() {
    return rootView;
  }

  @NonNull
  public static FragmentChatListBinding inflate(@NonNull LayoutInflater inflater) {
    return inflate(inflater, null, false);
  }

  @NonNull
  public static FragmentChatListBinding inflate(@NonNull LayoutInflater inflater,
      @Nullable ViewGroup parent, boolean attachToParent) {
    View root = inflater.inflate(R.layout.fragment_chat_list, parent, false);
    if (attachToParent) {
      parent.addView(root);
    }
    return bind(root);
  }

  @NonNull
  public static FragmentChatListBinding bind(@NonNull View rootView) {
    // The body of this method is generated in a way you would not otherwise write.
    // This is done to optimize the compiled bytecode for size and performance.
    int id;
    missingId: {
      id = R.id.app_bar_layout;
      AppBarLayout appBarLayout = rootView.findViewById(id);
      if (appBarLayout == null) {
        break missingId;
      }

      id = R.id.btn_create_chat;
      FloatingActionButton btnCreateChat = rootView.findViewById(id);
      if (btnCreateChat == null) {
        break missingId;
      }

      id = R.id.iv_profile_avatar;
      ImageView ivProfileAvatar = rootView.findViewById(id);
      if (ivProfileAvatar == null) {
        break missingId;
      }

      id = R.id.profile_content;
      LinearLayout profileContent = rootView.findViewById(id);
      if (profileContent == null) {
        break missingId;
      }

      id = R.id.rv_chat_list;
      RecyclerView rvChatList = rootView.findViewById(id);
      if (rvChatList == null) {
        break missingId;
      }

      id = R.id.toolbar;
      Toolbar toolbar = rootView.findViewById(id);
      if (toolbar == null) {
        break missingId;
      }

      id = R.id.tv_profile_name;
      TextView tvProfileName = rootView.findViewById(id);
      if (tvProfileName == null) {
        break missingId;
      }

      return new FragmentChatListBinding((CoordinatorLayout) rootView, appBarLayout, btnCreateChat,
          ivProfileAvatar, profileContent, rvChatList, toolbar, tvProfileName);
    }
    String missingId = rootView.getResources().getResourceName(id);
    throw new NullPointerException("Missing required view with ID: ".concat(missingId));
  }
}
