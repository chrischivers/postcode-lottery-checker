package com.postcodelotterychecker.models

case class Postcode(value: String) {
  def isValid: Boolean = {
    val postcodeRegEx = "^([Gg][Ii][Rr] 0[Aa]{2})|((([A-Za-z][0-9]{1,2})|(([A-Za-z][A-Ha-hJ-Yj-y][0-9]{1,2})|(([AZa-z][0-9][A-Za-z])|([A-Za-z][A-Ha-hJ-Yj-y][0-9]?[A-Za-z]))))[0-9][A-Za-z]{2})$".r
    postcodeRegEx.findFirstIn(value).isDefined
  }

  def trim: Postcode = {
    this.copy(value = value.replace("\n", "").replace(" ", "").trim)
  }
}