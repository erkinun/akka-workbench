package com.commutopia

import akka.http.scaladsl.marshalling.{ToResponseMarshallable, ToResponseMarshaller}


object Marshallers {
  class PersonMarshallers extends ToResponseMarshallable {
    override type T = this.type

    override def value: PersonMarshallers.this.type = ???

    override implicit def marshaller: ToResponseMarshaller[PersonMarshallers.this.type] = ???
  }
}
