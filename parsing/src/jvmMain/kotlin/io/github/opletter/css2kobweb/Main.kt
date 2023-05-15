package io.github.opletter.css2kobweb


fun main() {
    println(css2kobwebAsCode(rawCss3).joinToString(""))
//    println(css2kobweb(rawCSS2))
}

val rawCSS = """
a {
  color: red;
  text-decoration: none;
}

.button, .test {
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

.button:focus, .button:hover, .test:focus {
  background-color: #0056b3;
}
.button:focus-visible, .button > {
  background-color: rgb(0% 20% 50%);
}
.also-like-items > button > img {
  background-color: rgb(100% 0% 0%);
}

.screen:after,
.screen:before {
  content: "";
  height: 5px;
  position: absolute;
  z-index: 4;
  left: 50%;
  translate: -50% 0%;
  background-color: white;
}

.screen:before, .screen:x {
  width: 15%;
  top: 0rem;
  border-bottom-left-radius: 1rem;
  border-bottom-right-radius: 1rem;
}

.screen:before, .screen:hover {
  width: 19%;
}

.screen:before {
    width: 19%;
}

.screen:z {
    height: 100%;
}
.screen:z {
    width: 200%;
}
.screen:z {
    border-radius: 0;
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

val rawCss3 = """
    body {
      background-color: rgb(0,0,0);
      margin: 0px;
    }

    body::-webkit-scrollbar {
      width: 4px;
    }

    body::-webkit-scrollbar-track {
      background-color: rgb(1,1,1);
    }

    body::-webkit-scrollbar-thumb {
      background: rgba(255, 255, 255, 0.15);
    }

    * {
      box-sizing: border-box;
      margin: 0;
      padding: 0;
    }

    button {
      all: unset;
      cursor: pointer;
    }

    h1, h2, h3, h4, input, select, button, span, a, p {
      color: rgb(2,2,2);
      font-family: "Noto Sans", sans-serif;
      font-size: 1rem;
    }

    button, a, input {  
      outline: none;
    }

    .highlight {
      color: rgb(3,3,3);
    }

    .gradient {  
      background-image: rgb(4,4,4);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
    }

    .fancy-scrollbar::-webkit-scrollbar {
      height: 4px;
      width: 4px;
    }

    .fancy-scrollbar::-webkit-scrollbar-track {
      background-color: transparent;
    }

    .fancy-scrollbar::-webkit-scrollbar-thumb {
      background: rgba(255, 255, 255, 0.15);
    }

    .no-scrollbar::-webkit-scrollbar {
      height: 0px;
      width: 0px;
    }

    #phone {
      box-shadow: rgba(0, 0, 0, 0.2) 0px 8px 24px;
      height: 851px;
      width: 393px;  
      margin: 100px auto;
      position: relative;
      overflow: hidden;
    }

    #main-wrapper {
      height: 100%;
      overflow: auto;
    }

    #main {
      height: 100%;
    }

    #nav {
      width: 100%;
      display: flex;
      justify-content: space-around;
      position: absolute;
      left: 0px;
      bottom: 0px;
      z-index: 3;
      padding: 0.5rem 1rem;
      border-top: 1px solid rgb(255 255 255 / 10%);
    }

    #nav > button {
      padding: 0.5rem 1rem;  
      border-radius: 0.25rem;
      position: relative;
    }

    #nav > button.active:after {
      content: "";
      height: 0.25rem;
      width: 1.5rem;
      position: absolute;
      top: -0.5rem;
      left: 50%;
      translate: -50%;
      border-bottom-left-radius: 0.25rem;
      border-bottom-right-radius: 0.25rem;
    }

    #nav > button:hover,
    #nav > button:focus-visible {
      background-color: rgb(255 255 255 / 10%);
    }

    #nav > button > i {
      width: 1.5rem;
      font-size: 1.1rem;
      text-align: center;
    }

    #header {
      display: flex;
      flex-direction: column;
      width: 100%;
      overflow: hidden;
      position: relative;
    }

    #header-background-image {
      width: 100%;
      display: flex;
      z-index: 1;
      left: 0px;
      top: 0px;
      position: relative;
    }

    #header-background-image > img {
      height: 100%;
      width: 100%;
      object-fit: cover;
      object-position: center;
    }

    #header-items {
      display: flex;
      gap: 1rem;
      position: relative;
      z-index: 3;
      padding: 0.5rem 1rem;
      overflow: auto;
      background: linear-gradient(to bottom, rgb(34, 123, 66) 0%, rgb(10 10 10) 40%, transparent 40%);
    }

    .header-item-image {
      position: relative;  
    }

    .header-item-image:after {
      content: "";
      height: calc(100% - 0.5rem);
      width: calc(100% - 0.5rem);
      position: absolute;
      left: 0px;
      top: 0px;  
      z-index: -1;
      background-color: white;
      margin: -0.25rem;
      border-radius: 0.5rem;
      box-shadow: rgba(99, 99, 99, 0.2) 0px 2px 8px 0px;
    }

    .header-item-image > img {
      width: 200px;
      aspect-ratio: 16 / 9;
      object-fit: cover;
      object-position: center;
      border-radius: 0.4rem;
    }

    .header-item-content > .label {
      display: flex;  
    }

    .header-item-content > .label > p {
      color: white;
      font-size: 0.75rem;
      font-weight: 500;
      margin-left: 0.25rem;
    }

    #mainBody {
      display: flex;
      flex-direction: column;
      gap: 1rem;
    }

    #body-content {
      display: flex;
      flex-direction: column;
      gap: 1rem;
      position: relative;
      z-index: 2;
      margin-top: 1rem;
      padding-bottom: 4rem;
    }

    #background-gradient {
      height: 300px;
      width: 100%;
      position: absolute;
      z-index: 1;
      background: linear-gradient(
        -15deg, 
        transparent 30%, 
      );
      opacity: 0.15;
      filter: blur(3rem);
    }

    #search-wrapper {
      display: flex;
      flex-direction: column;
      gap: 0.25rem;
      margin: 0rem 1rem;
    }

    #search {
      height: 3.5rem;
      position: relative;
      border-radius: 0.4rem;
    }

    #search-input {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      position: absolute;
      inset: 2px;
      padding: 0.5rem;
      border-radius: 0.3rem;
      backdrop-filter: blur(0.75rem);
    }

    #search-input > i {
      width: 1.5rem;
      padding: 0rem 0.25rem;
      color: rgb(255 255 255 / 85%); 
      text-align: center;
    } 

    #search-input > input {
      width: 100%;
      flex-grow: 1; 
      color: white;
      background-color: transparent;
      border: none;
      outline: none;
      font-weight: 500;
    }

    #search-input > button {
      height: 2rem;
      width: 2rem;
      display: grid;
      place-items: center;
      flex-shrink: 0;
      cursor: pointer;
    }

    #search-input > button > i {
      color: rgb(255 255 255 / 85%); 
    }

    #search-input > button:is(:hover, :focus-visible) {
      background: rgb(255 255 255 / 10%);
      border-radius: 0.25rem;
    }

    #search-input > input::placeholder {
      color: rgb(255 255 255 / 25%);
    }

    #search-categories {
      display: flex;
      gap: 0.25rem;
      margin-bottom: 0.25rem;
      overflow: auto;
    }

    #search-categories > button {
      flex-shrink: 0;
      background-color: rgb(255 255 255 / 5%);
      padding: 0.5rem 0.75rem;
      border-radius: 0.25rem;
      color: white; 
      font-size: 0.75rem;
      font-weight: 500;
    }

    #location > button {
      height: 2rem;
      display: flex;
      align-items: center;
      gap: 0.4rem;
      margin-left: 2.25rem;
      position: relative;
    }

    #location > button:after {
      content: "";
      height: 0.75rem;
      width: 0.5rem;
      position: absolute;
      left: 0px;
      top: 0px;
      margin-left: -1.25rem;
      margin-top: 0.25rem;
      border-left: 2px solid rgb(255 255 255 / 40%);
      border-bottom: 2px solid rgb(255 255 255 / 40%);
      border-bottom-left-radius: 0.3rem;
    }

    #location > button > :is(i, p) {
      display: flex;
      align-items: center;
      height: 100%;
      color: white;
      font-size: 0.75rem;  
    }

    #location > button > i {
      height: 100%;
    }

    #location > button > p {
      color: rgb(5,5,5 / 75%);
      font-weight: 500;
    }

    #ad {
      display: flex;
      border: 1px solid rgb(255 255 255 / 10%);
      padding: 0.25rem;
      margin: 0rem 1rem;
      border-radius: 0.25rem;
    }

    #ad > img {
      width: 100%;
      border-radius: inherit;
    }

    #also-like {
      display: flex;
      flex-direction: column;
      gap: 0.5rem;
      margin: 0rem 1rem;
    }

    #also-like > h3 {
      font-size: 0.9rem; 
    }

    #also-like-items {
      display: grid;
      gap: 0.5rem;
      grid-template-columns: repeat(3, auto);
      grid-template-rows: repeat(3, 1fr);
    }

    #also-like-items > button { 
      display: flex;
      aspect-ratio: 1;
    }

    #also-like-items > button > img {
      height: 100%;
      width: 100%;
      object-fit: cover;
      border-radius: 0.25rem;
    }

    @media(max-width: 500px) {
      body {
        overflow: auto;  
      }
      
      #phone {
        height: 100vh;
        display: flex;
        width: 100%;
        margin: 0px auto;
      }
      
      #main-wrapper {
        width: 100%;
        flex-grow: 1;
      }
    }
""".trimIndent()


val rawCss4 = """
    #search-input {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      position: absolute;
      inset: 2px;
      padding: 0.5rem;
      border-radius: 0.3rem;
      backdrop-filter: blur(0.75rem);
    }

    #search-input > i {
      width: 1.5rem;
      padding: 0rem 0.25rem;
      color: rgb(255 255 255 / 85%); 
      text-align: center;
    } 

    #search-input > input {
      width: 100%;
      flex-grow: 1; 
      color: white;
      background-color: transparent;
      border: none;
      outline: none;
      font-weight: 500;
    }

    #search-input > button, #search-input > custom {
      height: 2rem;
      width: 2rem;
      display: grid;
      place-items: center;
      flex-shrink: 0;
      cursor: pointer;
    }

    #search-input > button > i {
      color: rgb(255 255 255 / 85%); 
    }

    #search-input > button:is(:hover, :focus-visible) {
      background: rgb(255 255 255 / 10%);
      border-radius: 0.25rem;
    }

    #search-input > input::placeholder {
      color: rgb(255 255 255 / 25%);
    }
""".trimIndent()