package io.dropwizard.logging;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.junit.Test;

import static org.fest.assertions.api.Assertions.assertThat;

public class FilteredAppenderFactoryTest {
  @Test
  public void isFiltering() throws Exception {
    for (FilterReply reply : FilterReply.values()) {
      FilteredAppenderFactory filteredAppenderFactory = new FilteredAppenderFactory();
      filteredAppenderFactory.setFilterReply(reply);

      Appender<ILoggingEvent> appender = filteredAppenderFactory
          .build(new LoggerContext(), "MyApplication", null);

      assertThat(appender.getFilterChainDecision(new LoggingEvent())).isEqualTo(reply);
    }
  }

  @JsonTypeName("filtered")
  class FilteredAppenderFactory extends AbstractAppenderFactory {
    private FilterReply reply;

    @JsonProperty
    public void setFilterReply(FilterReply reply) {
      this.reply = reply;
    }

    @Override
    public Appender<ILoggingEvent> build(LoggerContext context, String applicationName, Layout<ILoggingEvent> layout) {

      final MyAppender appender = new MyAppender();
      appender.setName("filtered-appender");
      appender.setContext(context);

      Filter<ILoggingEvent> filter = new Filter<ILoggingEvent>() {
        @Override
        public FilterReply decide(ILoggingEvent event) {
          return reply;
        }
      };
      filter.start();

      // Adding a filter to original Appender
      appender.addFilter(filter);
      appender.start();

      // AsyncAppender does not have any Filters
      Appender<ILoggingEvent> async = wrapAsync(appender);

      // AsyncAppender now has a filter
      // FIXME: This fixes the assertions
      // async.addFilter(filter);

      return async;
    }

    class MyAppender extends AppenderBase<ILoggingEvent> {
      @Override
      protected void append(ILoggingEvent eventObject) {
        System.err.print("got event");
      }
    }
  }
}
