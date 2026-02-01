package es.eriktorr
package common.types

import common.errors.HandledError

enum RefinedError(val message: String) extends HandledError(message):
  case EmptyOrBlankString(fieldName: String)
      extends RefinedError(s"$fieldName cannot be empty or blank")
