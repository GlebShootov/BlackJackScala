package com.evolution.blackjack.domain.data

//object CardValueType extends Enumeration {
//  type CardValueType = Value
//  val _2, _3, _4, _5, _6, _7, _8, _9, _10, J, Q, K, A = Value
//  val cardValues: List[CardValueType] = List(_2, _3, _4, _5, _6, _7, _8, _9, _10, J, Q, K, A)
//}

sealed trait CardValue {
  def cardId: Int
  def cardName: String

  override def toString = s"CardValue.$cardId" // TODO: check
}

object CardValue {
  def cardValues: Set[CardValue] = {
    Set(
      `2`, `3`, `4`, `5`, `6`, `7`, `8`, `9`, `10`, Jack, Queen, King, Ace
    )
  }

  case object `2` extends CardValue {
    val cardId: Int = 2
    val cardName: String = "2"
  }

  case object `3` extends CardValue {
    val cardId: Int = 3
    val cardName: String = "3"

    override val toString = s"CardValue.$cardName" // TODO: remove
  }

  case object `4` extends CardValue {
    val cardId: Int = 4
    val cardName: String = "4"

    override val toString = s"CardValue.$cardName"
  }

  case object `5` extends CardValue {
    val cardId: Int = 5
    val cardName: String = "5"

    override val toString = s"CardValue.$cardName"
  }

  case object `6` extends CardValue {
    val cardId: Int = 6
    val cardName: String = "6"

    override val toString = s"CardValue.$cardName"
  }

  case object `7` extends CardValue {
    val cardId: Int = 7
    val cardName: String = "7"

    override val toString = s"CardValue.$cardName"
  }

  case object `8` extends CardValue {
    val cardId: Int = 8
    val cardName: String = "8"

    override val toString = s"CardValue.$cardName"
  }

  case object `9` extends CardValue {
    val cardId: Int = 9
    val cardName: String = "9"

    override val toString = s"CardValue.$cardName"
  }

  case object `10` extends CardValue {
    val cardId: Int = 10
    val cardName: String = "10"

    override val toString = s"CardValue.$cardName"
  }

  case object Jack extends CardValue {
    val cardId: Int = 11
    val cardName: String = "Jack"

    override val toString = s"CardValue.$cardName"
  }

  case object Queen extends CardValue {
    val cardId: Int = 12
    val cardName: String = "Queen"

    override val toString = s"CardValue.$cardName"
  }

  case object King extends CardValue {
    val cardId: Int = 13
    val cardName: String = "King"

    override val toString = s"CardValue.$cardName"
  }

  case object Ace extends CardValue {
    val cardId: Int = 14
    val cardName: String = "Ace"

    override val toString = s"CardValue.$cardName"
  }
}
