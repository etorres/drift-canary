package es.eriktorr
package attribution

import attribution.model.{Attribution, Event}

object types:
  type TestCase[Input, Filter, Output] =
    (
        items: Input,
        filter: Filter,
        expected: Output,
    )

  type EventTestCase[Filter, Output] =
    TestCase[List[Event], Filter, Output]

  type AttributionTestCase[Filter, Output] =
    TestCase[(List[Event], List[Attribution]), Filter, Output]
