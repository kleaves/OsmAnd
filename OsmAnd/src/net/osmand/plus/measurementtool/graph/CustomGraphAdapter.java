package net.osmand.plus.measurementtool.graph;

import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.graphics.ColorUtils;

import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

import net.osmand.AndroidUtils;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.SettingsNavigationActivity;
import net.osmand.plus.helpers.CustomBarChartRenderer;
import net.osmand.plus.helpers.GpxUiHelper;
import net.osmand.router.RouteStatisticsHelper;
import net.osmand.router.RouteStatisticsHelper.RouteStatistics;
import net.osmand.router.RouteStatisticsHelper.RouteSegmentAttribute;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CustomGraphAdapter extends BaseGraphAdapter<HorizontalBarChart, BarData, RouteStatistics> {

	private static final int MINIMUM_CONTRAST_RATIO = 3;

	private String selectedPropertyName;
	private ViewGroup legendContainer;
	private LegendViewType legendViewType;
	private LayoutChangeListener layoutChangeListener;

	public enum LegendViewType {
		ONE_ELEMENT,
		ALL_AS_LIST,
		GONE
	}

	public CustomGraphAdapter(HorizontalBarChart chart, boolean usedOnMap) {
		super(chart, usedOnMap);
	}

	@Override
	protected void prepareCharterView() {
		super.prepareCharterView();
		legendViewType = LegendViewType.GONE;
		mChart.setRenderer(new CustomBarChartRenderer(mChart));
		mChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
			@Override
			public void onValueSelected(Entry e, Highlight h) {
				if (getStatistics() == null) return;

				List<RouteStatisticsHelper.RouteSegmentAttribute> elems = getStatistics().elements;
				int i = h.getStackIndex();
				if (i >= 0 && elems.size() > i) {
					selectedPropertyName = elems.get(i).getPropertyName();
					updateLegend();
				}
			}

			@Override
			public void onNothingSelected() {
				selectedPropertyName = null;
				updateLegend();
			}
		});
	}

	@Override
	public void updateView() {
		mChart.clear();
		GpxUiHelper.setupHorizontalGPXChart(getMyApplication(), mChart, 5, 9, 24, true, isNightMode());
		mChart.setData(mChartData);
		updateHighlight();
		updateLegend();
	}

	public void setLegendContainer(ViewGroup legendContainer) {
		this.legendContainer = legendContainer;
	}

	public void setLegendViewType(LegendViewType legendViewType) {
		this.legendViewType = legendViewType;
	}

	public void setLayoutChangeListener(LayoutChangeListener layoutChangeListener) {
		this.layoutChangeListener = layoutChangeListener;
	}

	public void highlight(Highlight h) {
		super.highlight(h);
		Highlight bh = h != null ? mChart.getHighlighter().getHighlight(1, h.getXPx()) : null;
		if (bh != null) {
			bh.setDraw(h.getXPx(), 0);
		}
		mChart.highlightValue(bh, true);
	}

	private void updateLegend() {
		if (legendContainer == null) return;

		legendContainer.removeAllViews();
		attachLegend();
		if (layoutChangeListener != null) {
			layoutChangeListener.onLayoutChanged();
		}
	}

	private void attachLegend() {
		if (getSegmentsList() == null) return;

		switch (legendViewType) {
			case ONE_ELEMENT:
				for (RouteSegmentAttribute segment : getSegmentsList()) {
					if (segment.getPropertyName().equals(selectedPropertyName)) {
						attachLegend(Arrays.asList(segment), null);
						break;
					}
				}
				break;
			case ALL_AS_LIST:
				attachLegend(getSegmentsList(), selectedPropertyName);
				break;
		}
	}

	private void attachLegend(List<RouteSegmentAttribute> list,
	                          String propertyNameToFullSpan) {
		OsmandApplication app = getMyApplication();
		LayoutInflater inflater = LayoutInflater.from(app);
		for (RouteStatisticsHelper.RouteSegmentAttribute segment : list) {
			View view = inflater.inflate(R.layout.route_details_legend, legendContainer, false);
			int segmentColor = segment.getColor();
			Drawable circle = app.getUIUtilities().getPaintedIcon(R.drawable.ic_action_circle, segmentColor);
			ImageView legendIcon = (ImageView) view.findViewById(R.id.legend_icon_color);
			legendIcon.setImageDrawable(circle);
			double contrastRatio = ColorUtils.calculateContrast(segmentColor,
					AndroidUtils.getColorFromAttr(app, R.attr.card_and_list_background_basic));
			if (contrastRatio < MINIMUM_CONTRAST_RATIO) {
				legendIcon.setBackgroundResource(AndroidUtils.resolveAttribute(app, R.attr.bg_circle_contour));
			}
			String propertyName = segment.getUserPropertyName();
			String name = SettingsNavigationActivity.getStringPropertyName(app, propertyName, propertyName.replaceAll("_", " "));
			boolean selected = segment.getPropertyName().equals(propertyNameToFullSpan);
			Spannable text = getSpanLegend(name, segment, selected);
			TextView legend = (TextView) view.findViewById(R.id.legend_text);
			legend.setText(text);

			legendContainer.addView(view);
		}
	}

	private Spannable getSpanLegend(String title,
	                                RouteSegmentAttribute segment,
	                                boolean fullSpan) {
		String formattedDistance = OsmAndFormatter.getFormattedDistance(segment.getDistance(), getMyApplication());
		title = Algorithms.capitalizeFirstLetter(title);
		SpannableStringBuilder spannable = new SpannableStringBuilder(title);
		spannable.append(": ");
		int startIndex = fullSpan ? -0 : spannable.length();
		spannable.append(formattedDistance);
		spannable.setSpan(new StyleSpan(android.graphics.Typeface.BOLD),
				startIndex, spannable.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		return spannable;
	}

	private List<RouteSegmentAttribute> getSegmentsList() {
		return getStatistics() != null ? new ArrayList<>(getStatistics().partition.values()) : null;
	}

	private RouteStatistics getStatistics() {
		return mAdditionalData;
	}
}
