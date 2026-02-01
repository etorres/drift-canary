package es.eriktorr
package attribution.api

import spec.TestFilters.{delayed, scheduled}

import munit.FunSuite

final class DriftCanarySuite extends FunSuite:
  test("should run canary check".tag(scheduled)):
    fail("not implemented")

  test("should run canary check".tag(delayed)):
    fail("not implemented")

/* TODO
Event selector & uploader
- Select from the "purchase_prod" up to 10 events uploaded two days ago, along with their attributions.
- Upload the events to the "purchase_test" conversion action.

Attributions verifier
- Get the attribution for the 10 events.
- Compare the attributions to the "purchase_prod".
 */
