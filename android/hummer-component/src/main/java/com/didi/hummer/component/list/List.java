package com.didi.hummer.component.list;

import android.content.Context;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.LayoutInflater;
import android.view.MotionEvent;

import com.didi.hummer.annotation.Component;
import com.didi.hummer.annotation.JsAttribute;
import com.didi.hummer.annotation.JsMethod;
import com.didi.hummer.annotation.JsProperty;
import com.didi.hummer.component.R;
import com.didi.hummer.component.input.FocusUtil;
import com.didi.hummer.component.list.decoration.GridSpacingItemDecoration;
import com.didi.hummer.component.list.decoration.LinearSpacingItemDecoration;
import com.didi.hummer.component.list.decoration.StaggeredGridSpacingItemDecoration;
import com.didi.hummer.component.refresh.HummerFooter;
import com.didi.hummer.component.refresh.HummerHeader;
import com.didi.hummer.component.refresh.LoadMoreState;
import com.didi.hummer.component.refresh.PullRefreshState;
import com.didi.hummer.context.HummerContext;
import com.didi.hummer.core.engine.JSCallback;
import com.didi.hummer.core.engine.JSValue;
import com.didi.hummer.pool.ObjectPool;
import com.didi.hummer.render.component.view.HMBase;
import com.didi.hummer.render.event.view.ScrollEvent;
import com.didi.hummer.render.style.HummerStyleUtils;
import com.didi.hummer.render.utility.DPUtil;
import com.scwang.smart.refresh.layout.SmartRefreshLayout;

import java.util.Map;

/**
 * 列表组件（包含垂直和水平）
 *
 * Created by XiaoFeng on 2020-09-30.
 */
@Component("List")
public class List extends HMBase<SmartRefreshLayout> {
    private static final int MODE_LIST = 1;
    private static final int MODE_GRID = 2;
    private static final int MODE_WATERFALL = 3;

    private static final int DIRECTION_VERTICAL = 1;
    private static final int DIRECTION_HORIZONTAL = 2;

    private int mode = MODE_LIST;
    private int direction = DIRECTION_VERTICAL;
    private int column = 2;
    private int lineSpacing;
    private int itemSpacing;
    private int leftSpacing;
    private int rightSpacing;
    private int topSpacing;
    private int bottomSpacing;
    private boolean showScrollBar;

    private SmartRefreshLayout refreshLayout;
    private HummerHeader hummerHeader;
    private HummerFooter hummerFooter;
    private RecyclerView recyclerView;
    private HMListAdapter adapter;
    private RecyclerView.LayoutManager layoutManager;

    private ObjectPool instanceManager;

    private boolean isLoadingMore;

    private boolean isScrollStarted = false;
    private ScrollEvent scrollEvent = new ScrollEvent();

    private RecyclerView.OnScrollListener mOnScrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            if (!mEventManager.contains(ScrollEvent.HM_EVENT_TYPE_SCROLL)) {
                return;
            }

            if (!isScrollStarted) {
                isScrollStarted = true;
                // 开始滑动
                scrollEvent.setType(ScrollEvent.HM_EVENT_TYPE_SCROLL);
                scrollEvent.setState(ScrollEvent.HM_SCROLL_STATE_BEGAN);
                scrollEvent.setOffsetX(0);
                scrollEvent.setOffsetY(0);
                scrollEvent.setDx(0);
                scrollEvent.setDy(0);
                scrollEvent.setTimestamp(System.currentTimeMillis());
                mEventManager.dispatchEvent(ScrollEvent.HM_EVENT_TYPE_SCROLL, scrollEvent);
            }

            int offsetX = recyclerView.computeHorizontalScrollOffset();
            int offsetY = recyclerView.computeVerticalScrollOffset();

            scrollEvent.setType(ScrollEvent.HM_EVENT_TYPE_SCROLL);
            scrollEvent.setState(ScrollEvent.HM_SCROLL_STATE_SCROLL);
            scrollEvent.setOffsetX(offsetX);
            scrollEvent.setOffsetY(offsetY);
            scrollEvent.setDx(dx);
            scrollEvent.setDy(dy);
            scrollEvent.setTimestamp(System.currentTimeMillis());
            mEventManager.dispatchEvent(ScrollEvent.HM_EVENT_TYPE_SCROLL, scrollEvent);
        }

        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
            if (!mEventManager.contains(ScrollEvent.HM_EVENT_TYPE_SCROLL)) {
                return;
            }

            if (newState == RecyclerView.SCROLL_STATE_SETTLING) {
                scrollEvent.setType(ScrollEvent.HM_EVENT_TYPE_SCROLL);
                scrollEvent.setState(ScrollEvent.HM_SCROLL_STATE_SCROLL_UP);
                scrollEvent.setTimestamp(System.currentTimeMillis());
                mEventManager.dispatchEvent(ScrollEvent.HM_EVENT_TYPE_SCROLL, scrollEvent);
            } else if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                scrollEvent.setType(ScrollEvent.HM_EVENT_TYPE_SCROLL);
                scrollEvent.setState(ScrollEvent.HM_SCROLL_STATE_ENDED);
                scrollEvent.setTimestamp(System.currentTimeMillis());
                mEventManager.dispatchEvent(ScrollEvent.HM_EVENT_TYPE_SCROLL, scrollEvent);
            }
        }
    };

    public List(HummerContext context, JSValue jsValue, String viewID) {
        super(context, jsValue, viewID);
        this.instanceManager = context.getObjectPool();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (adapter != null) {
            adapter.destroy();
        }
    }

    @Override
    protected SmartRefreshLayout createViewInstance(Context context) {
        // 这里不用代码new一个RecyclerView，而是通过xml，是为了解决设置scrollerbar显示无效的问题
        recyclerView = (RecyclerView) LayoutInflater.from(context).inflate(R.layout.recycler_view, null, false);
        recyclerView.setOnTouchListener((v, event) -> {
            // 手指按下时，如果有键盘已弹出，则把键盘消失掉
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                FocusUtil.clearFocus(context);
            }
            return false;
        });

        refreshLayout = new SmartRefreshLayout(context);
        refreshLayout.setEnableRefresh(false);
        refreshLayout.setEnableLoadMore(false);
        refreshLayout.setRefreshContent(recyclerView);

        hummerHeader = new HummerHeader(context);
        hummerFooter = new HummerFooter(context);
        refreshLayout.setRefreshHeader(hummerHeader);
        refreshLayout.setRefreshFooter(hummerFooter);

        hummerHeader.setOnRefreshListener(new HummerHeader.OnRefreshListener() {
            @Override
            public void onRefreshStarted() {
                if (refreshCallback != null) {
                    refreshCallback.call(PullRefreshState.START_PULL_DOWN);
                }
            }

            @Override
            public void onRefreshing() {
                if (refreshCallback != null) {
                    refreshCallback.call(PullRefreshState.REFRESHING);
                }
            }

            @Override
            public void onRefreshFinished() {
                if (refreshCallback != null) {
                    refreshCallback.call(PullRefreshState.IDLE);
                }
            }
        });

        hummerFooter.setOnLoadListener(new HummerFooter.OnLoadListener() {
            @Override
            public void onLoadStarted() {

            }

            @Override
            public void onLoading() {
                isLoadingMore = true;
                if (loadMoreCallback != null) {
                    loadMoreCallback.call(LoadMoreState.LOADING);
                }
            }

            @Override
            public void onLoadFinished() {
                isLoadingMore = false;
            }
        });

        return refreshLayout;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        recyclerView.addOnScrollListener(mOnScrollListener);
    }

    private void bindRecyclerViewIfNeed() {
        if (adapter != null) {
            return;
        }

        initRecyclerLayoutManager();

        // List组件四周边缘的间距
        if (leftSpacing > 0 || rightSpacing > 0 || topSpacing > 0 || bottomSpacing > 0) {
            recyclerView.setPadding(leftSpacing, topSpacing, rightSpacing, bottomSpacing);
            recyclerView.setClipToPadding(false);
        }

        // 滚动条显示或隐藏
        if (direction == DIRECTION_VERTICAL) {
            recyclerView.setVerticalScrollBarEnabled(showScrollBar);
        } else if (direction == DIRECTION_HORIZONTAL) {
            recyclerView.setHorizontalScrollBarEnabled(showScrollBar);
        }

        adapter = new HMListAdapter(getContext(), instanceManager);
        recyclerView.setAdapter(adapter);
    }

    private void initRecyclerLayoutManager() {
        switch (mode) {
            case MODE_LIST:
            default:
                if (direction == DIRECTION_HORIZONTAL) {
                    layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
                } else {
                    layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
                }
                if (lineSpacing > 0) {
                    recyclerView.addItemDecoration(new LinearSpacingItemDecoration(lineSpacing, false));
                }
                break;
            case MODE_GRID:
                if (direction == DIRECTION_HORIZONTAL) {
                    layoutManager = new GridLayoutManager(getContext(), column, GridLayoutManager.HORIZONTAL, false);
                } else {
                    layoutManager = new GridLayoutManager(getContext(), column, GridLayoutManager.VERTICAL, false);
                }
                if (lineSpacing > 0 || itemSpacing > 0) {
                    recyclerView.addItemDecoration(new GridSpacingItemDecoration(column, lineSpacing, itemSpacing, false));
                }
                break;
            case MODE_WATERFALL:
                if (direction == DIRECTION_HORIZONTAL) {
                    layoutManager = new StaggeredGridLayoutManager(column, StaggeredGridLayoutManager.HORIZONTAL);
                } else {
                    layoutManager = new StaggeredGridLayoutManager(column, StaggeredGridLayoutManager.VERTICAL);
                }
                if (lineSpacing > 0 || itemSpacing > 0) {
                    recyclerView.addItemDecoration(new StaggeredGridSpacingItemDecoration(column, lineSpacing, itemSpacing, false));
                }
                break;
        }
        recyclerView.setLayoutManager(layoutManager);
    }

    @Override
    public void setStyle(Map style) {
        super.setStyle(style);
        bindRecyclerViewIfNeed();
    }

    @JsAttribute("mode")
    public void setMode(String strMode) {
        switch (strMode) {
            case "list":
            default:
                mode = MODE_LIST;
                break;
            case "grid":
                mode = MODE_GRID;
                break;
            case "waterfall":
                mode = MODE_WATERFALL;
                // 瀑布流模式下的默认值为8
                if (lineSpacing <= 0) {
                    lineSpacing = DPUtil.dp2px(getContext(), 8);
                }
                if (itemSpacing <= 0) {
                    itemSpacing = DPUtil.dp2px(getContext(), 8);
                }
                break;
        }
    }

    @JsAttribute("scrollDirection")
    public void setScrollDirection(String strDirection) {
        switch (strDirection) {
            case "vertical":
            default:
                direction = DIRECTION_VERTICAL;
                break;
            case "horizontal":
                direction = DIRECTION_HORIZONTAL;
                break;
        }
    }

    @JsAttribute("column")
    public void setColumn(int column) {
        this.column = column;
    }

    @JsAttribute("lineSpacing")
    public void setLineSpacing(int spacing) {
        this.lineSpacing = spacing;
    }

    @JsAttribute("itemSpacing")
    public void setItemSpacing(int spacing) {
        this.itemSpacing = spacing;
    }

    @JsAttribute("leftSpacing")
    public void setLeftSpacing(int spacing) {
        this.leftSpacing = spacing;
    }

    @JsAttribute("rightSpacing")
    public void setRightSpacing(int spacing) {
        this.rightSpacing = spacing;
    }

    @JsAttribute("topSpacing")
    public void setTopSpacing(int spacing) {
        this.topSpacing = spacing;
    }

    @JsAttribute("bottomSpacing")
    public void setBottomSpacing(int spacing) {
        this.bottomSpacing = spacing;
    }

    @JsAttribute("showScrollBar")
    public void setShowScrollBar(boolean hidden) {
        this.showScrollBar = hidden;
    }

    @JsProperty("refreshView")
    private HMBase refreshView;
    public void setRefreshView(HMBase view) {
        refreshLayout.setEnableRefresh(true);
        hummerHeader.addHeaderView(view);
    }

    @JsProperty("loadMoreView")
    private HMBase loadMoreView;
    public void setLoadMoreView(HMBase view) {
        refreshLayout.setEnableLoadMore(true);
        hummerFooter.addFooterView(view);
    }

    @JsProperty("onRefresh")
    private JSCallback refreshCallback;
    public void setOnRefresh(JSCallback callback) {
        refreshCallback = callback;
    }

    @JsProperty("onLoadMore")
    private JSCallback loadMoreCallback;
    public void setOnLoadMore(JSCallback callback) {
        loadMoreCallback = callback;
    }

    @JsProperty("onRegister")
    private JSCallback onRegister;
    public void setOnRegister(JSCallback onRegister) {
        adapter.setTypeCallback(onRegister);
    }

    @JsProperty("onCreate")
    private JSCallback onCreate;
    public void setOnCreate(JSCallback onCreate) {
        adapter.setCreateCallback(onCreate);
    }

    @JsProperty("onUpdate")
    private JSCallback onUpdate;
    public void setOnUpdate(JSCallback onUpdate) {
        adapter.setUpdateCallback(onUpdate);
    }

    @JsMethod("refresh")
    public void refresh(int count) {
        refreshLayout.setNoMoreData(false);
        if (adapter != null) {
            adapter.refresh(count, isLoadingMore);
        }
        isLoadingMore = false;
    }

    @JsMethod("stopPullRefresh")
    public void stopPullRefresh() {
        // 这里加一个delay时间，是为了解决RefreshFinish状态回调有时候不来的问题
        refreshLayout.finishRefresh(30);
    }

    @JsMethod("stopLoadMore")
    public void stopLoadMore(boolean enable) {
        if (enable) {
            refreshLayout.finishLoadMore();
        } else {
            refreshLayout.finishLoadMoreWithNoMoreData();
        }

        if (loadMoreCallback != null) {
            loadMoreCallback.call(enable ? LoadMoreState.IDLE : LoadMoreState.NO_MORE_DATA);
        }

        isLoadingMore = false;
    }

    @JsMethod("scrollTo")
    public void scrollTo(int x, int y) {
        recyclerView.scrollTo(x, y);
    }

    @JsMethod("scrollBy")
    public void scrollBy(int dx, int dy) {
        recyclerView.scrollBy(dx, dy);
    }

    @JsMethod("scrollToPosition")
    public void scrollToPosition(int position) {
        recyclerView.scrollToPosition(position);
    }

    @Override
    public void resetStyle() {
        super.resetStyle();
        setMode("list");
        setScrollDirection("vertical");
        setColumn(2);
        setShowScrollBar(false);
    }

    @Override
    public boolean setStyle(String key, Object value) {
        switch (key) {
            case HummerStyleUtils.Hummer.MODE:
                setMode(String.valueOf(value));
                break;
            case HummerStyleUtils.Hummer.SCROLL_DIRECTION:
                setScrollDirection(String.valueOf(value));
                break;
            case HummerStyleUtils.Hummer.COLUMN:
                setColumn((int) value);
                break;
            case HummerStyleUtils.Hummer.LINE_SPACING:
                setLineSpacing((int) (float) value);
                break;
            case HummerStyleUtils.Hummer.ITEM_SPACING:
                setItemSpacing((int) (float) value);
                break;
            case HummerStyleUtils.Hummer.LEFT_SPACING:
                setLeftSpacing((int) (float) value);
                break;
            case HummerStyleUtils.Hummer.RIGHT_SPACING:
                setRightSpacing((int) (float) value);
                break;
            case HummerStyleUtils.Hummer.TOP_SPACING:
                setTopSpacing((int) (float) value);
                break;
            case HummerStyleUtils.Hummer.BOTTOM_SPACING:
                setBottomSpacing((int) (float) value);
                break;
            case HummerStyleUtils.Hummer.SHOW_SCROLL_BAR:
                setShowScrollBar((boolean) value);
                break;
            default:
                return false;
        }
        return true;
    }
}

