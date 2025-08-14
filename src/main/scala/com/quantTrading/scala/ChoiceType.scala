package com.quantTrading.scala


sealed trait ChoiceType2[+T1, +T2]
sealed trait ChoiceType3[+T1, +T2, +T3]
sealed trait ChoiceType4[+T1, +T2, +T3, +T4]
sealed trait ChoiceType5[+T1, +T2, +T3, +T4, +T5]

final case class Choice1[+A](x: A) extends ChoiceType2[A, Nothing] with ChoiceType3[A, Nothing, Nothing] with ChoiceType4[A, Nothing, Nothing, Nothing] with ChoiceType5[A, Nothing, Nothing, Nothing, Nothing]
final case class Choice2[+B](x: B) extends ChoiceType2[Nothing, B] with ChoiceType3[Nothing, B, Nothing] with ChoiceType4[Nothing, B, Nothing, Nothing] with ChoiceType5[Nothing, B, Nothing, Nothing, Nothing]
final case class Choice3[+C](x: C) extends ChoiceType3[Nothing, Nothing, C] with ChoiceType4[Nothing, Nothing, C, Nothing] with ChoiceType5[Nothing, Nothing, C, Nothing, Nothing]
final case class Choice4[+D](x: D) extends ChoiceType4[Nothing, Nothing, Nothing, D] with ChoiceType5[Nothing, Nothing, Nothing, D, Nothing]
final case class Choice5[+E](x: E) extends ChoiceType5[Nothing, Nothing, Nothing, Nothing, E]
