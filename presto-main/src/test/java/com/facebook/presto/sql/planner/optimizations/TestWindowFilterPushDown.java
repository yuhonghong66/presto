/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.presto.sql.planner.optimizations;

import com.facebook.presto.sql.planner.assertions.BasePlanTest;
import com.facebook.presto.sql.planner.plan.FilterNode;
import com.facebook.presto.sql.planner.plan.TopNRowNumberNode;
import org.intellij.lang.annotations.Language;
import org.testng.annotations.Test;

import static com.facebook.presto.sql.planner.assertions.PlanMatchPattern.anyNot;
import static com.facebook.presto.sql.planner.assertions.PlanMatchPattern.anyTree;
import static com.facebook.presto.sql.planner.assertions.PlanMatchPattern.limit;
import static com.facebook.presto.sql.planner.assertions.PlanMatchPattern.node;
import static com.facebook.presto.sql.planner.assertions.PlanMatchPattern.tableScan;

public class TestWindowFilterPushDown
        extends BasePlanTest
{
    @Test
    public void testLimitAboveWindow()
    {
        @Language("SQL") String sql = "SELECT " +
                "row_number() OVER (PARTITION BY suppkey ORDER BY orderkey) partition_row_number FROM lineitem LIMIT 10";

        assertPlan(
                sql,
                anyTree(
                        limit(10, anyTree(
                                node(TopNRowNumberNode.class,
                                        anyTree(
                                                tableScan("lineitem")))))));
    }

    @Test
    public void testFilterAboveWindow()
    {
        @Language("SQL") String sql = "SELECT * FROM " +
                "(SELECT row_number() OVER (PARTITION BY suppkey ORDER BY orderkey) partition_row_number FROM lineitem) " +
                "WHERE partition_row_number < 10";

        assertPlan(
                sql,
                anyTree(
                        anyNot(FilterNode.class,
                                node(TopNRowNumberNode.class,
                                        anyTree(
                                                tableScan("lineitem"))))));
    }
}