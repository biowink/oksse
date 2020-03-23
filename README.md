## OkSSE
[![GitHub license](https://img.shields.io/github/license/mashape/apistatus.svg)](LICENSE.md)

OkSSE is an client for Server Sent events protocol written in Kotlin Multiplatform.
The implementation is written according to [W3C Recommendation 03 February 2015](https://www.w3.org/TR/2015/REC-eventsource-20150203)) specification.

OkSSE is an SSE client that's modern, efficient and provides an easy to use API.

## Usage

Check out [the sample for JVM](sample-jvm) to quickly test it on your desktop 

## Download

The [changelog](CHANGELOG.md) contains release history for all the releases.

### Android / JVM
Coming soon - the dependency

#### R8 / Proguard

OkSse doesn't require adding any extra rules for R8 / proguard. 

If you use Proguard, you may need to add rules for [Coroutines](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/resources/META-INF/proguard/coroutines.pro), [OkHttp](https://github.com/square/okhttp/blob/master/okhttp/src/main/resources/META-INF/proguard/okhttp3.pro) and [Okio](https://github.com/square/okio/blob/master/okio/src/jvmMain/resources/META-INF/proguard/okio.pro).

### Native
Not supported yet, happy to receive contributions :)

### JS
Not supported yet, happy to receive contributions :)

## Contributing

Please read [CONTRIBUTING.md](CONTRIBUTING.md) for contributions.

## License

    Copyright (c) 2020 Biowink GmbH
    
    Permission is hereby granted, free of charge, to any person obtaining a copy
    of this software and associated documentation files (the "Software"), to deal
    in the Software without restriction, including without limitation the rights
    to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
    copies of the Software, and to permit persons to whom the Software is
    furnished to do so, subject to the following conditions:
    
    The above copyright notice and this permission notice shall be included in all
    copies or substantial portions of the Software.
    
    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
    SOFTWARE.