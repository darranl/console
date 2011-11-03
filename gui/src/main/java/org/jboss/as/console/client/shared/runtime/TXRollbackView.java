package org.jboss.as.console.client.shared.runtime;

import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.shared.runtime.charts.RollbackChartView;
import org.jboss.as.console.client.shared.runtime.plain.RollbackPlainView;
import org.jboss.as.console.client.shared.runtime.plain.TXRollbackSampler;
import org.jboss.as.console.client.shared.subsys.tx.TransactionPresenter;

/**
 * @author Heiko Braun
 * @date 10/25/11
 */
public class TXRollbackView implements TXRollbackSampler {

    private TransactionPresenter presenter;
    private TXRollbackSampler sampler = null;

    @Deprecated
    public TXRollbackView(TransactionPresenter presenter) {
        this.presenter = presenter;
    }

    public TXRollbackView() {
        this.presenter = presenter;
    }

    public Widget asWidget() {
        return displayStrategy();
    }

    private Widget displayStrategy() {

        if(Console.visAPILoaded()) {
            sampler = new RollbackChartView(320, 200, "Rollback Origin");
        }
        else
        {
            sampler = new RollbackPlainView();
        }

        return sampler.asWidget();
    }

    @Override
    public void addSample(RollbackMetric metric) {
        sampler.addSample(metric);
    }

    @Override
    public void clearSamples() {
        sampler.clearSamples();
    }

    @Override
    public long numSamples() {
        return sampler.numSamples();
    }

    @Override
    public void recycle() {
        sampler.recycle();
    }
}