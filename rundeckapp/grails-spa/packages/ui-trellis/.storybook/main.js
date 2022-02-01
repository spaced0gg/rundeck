let path = require('path')

module.exports = {
  "stories": [
    "../src/**/*.stories.mdx",
    "../src/components/**/*.stories.@(js|jsx|ts)"
  ],
  "addons": [
    "@storybook/addon-knobs",
    "@storybook/addon-links",
    "@storybook/addon-essentials"
  ],
  "framework": "@storybook/vue",
  "core": {
    "builder": "webpack5"
  },
  webpackFinal: (config) => {
    // const vueLoader = config.module.rules.find(r => String(r.test) == String(/\.vue$/))
    // vueLoader.options.compilerOptions = {
    //     preserveWhitespace: false
    // }

    //const cssLoader = config.module.rules.find(r => String(r.test) == String(/\.css$/))
    //cssLoader.use[1].options.sourceMap = true

    //config.optimization.splitChunks = false

    //// config.devtool = 'eval-source-map'

 
   // Make whatever fine-grained changes you need
  
   config.module.rules = config.module.rules.filter(rule =>
     !rule.test.test('.scss')
   )
   config.module.rules.push(
     //{
     //  test: /\.ejs$/i,
     //  use: "html-loader",
     //},
    
     {
      test: /\.tsx$/,
      use: ["vue-loader", "vue-tsx-loader",'ts-loader'],
    },
     {
       test: /\.ts$/,
       exclude: /node_modules/,
       use: [{
           loader: 'ts-loader',
           options: {
           appendTsSuffixTo: [/\.vue$/],
           transpileOnly: true
           },
       }],
   },
   {
     test: /\.sass$/,
     use: [
       'vue-style-loader',
       'css-loader', {
         loader: 'sass-loader',
         options: {
           implementation: require('sass'),
        
         }
       },
     ],
   }, {
     test: /\.scss$/,
     use: [
       'vue-style-loader',
       'css-loader', {
         loader: 'sass-loader',
         options: {
           implementation: require('sass'),
           
         }
       },
     ],
   })
    // config.module.rules.push(
    //  {
    //      test: /\.ts$/,
    //      exclude: /node_modules/,
    //      use: [{
    //          loader: 'ts-loader',
    //          options: {
    //          appendTsSuffixTo: [/\.vue$/],
    //          transpileOnly: true
    //          },
    //      }],
    //  },
    // 
 
    //);

    /*config.plugins.unshift(new webpack.NormalModuleReplacementPlugin( /index.less/, function(resource) {
        if (resource.resource) {
            resource.resource = resource.resource.replace(/node_modules\/ant-design-vue\/es\/style\/index\.less/, 'src/antScope.less')
        }
    })) */

    return {
        ...config,
    };
  }
}