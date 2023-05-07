package io.github.opletter.css2kobweb

fun main() {
    val rawCSS = """
a {
  color: red;
  text-decoration: none;
}

.button {
  background-color: #007bff;
  color: #ffffff;
  padding: 10px 20px;
  border-radius: 0;
  text-transform: uppercase;
  font-weight: bold;
  text-align: center;
  display: inline-block;
  margin: 10px;
  cursor: pointer;
  transition: margin-right 2s, color 1s;
}

.button:hover {
  background-color: #0056b3;
}
    """.trimIndent()

    val rawCSS2 = """
  background-color: #007bff;
  color: #ffffff;
  padding: 10px 20px;
  border-radius: 0;
  text-transform: uppercase;
  font-weight: bold;
  text-align: center;
  display: inline-block;
  margin: 10px;
  cursor: pointer;
  transition: margin-right 2s, color 1s;
    """.trimIndent()

    println(css2kobweb(rawCSS))
    println(css2kobweb(rawCSS2))
}
