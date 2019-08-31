<template>
    <div>
      <v-layout class="pr-4 pb-3">
        <v-flex xs2>
          <v-btn color="blue" >新增品牌</v-btn>
        </v-flex>
        <v-spacer/>
        <v-flex xs4>
          <v-text-field label="搜索" append-icon="search" v-model="key"/>
        </v-flex>
      </v-layout>
      <v-data-table
        :headers="headers"
        :items="brands"
        :search="search"
        :pagination.sync="pagination"
        :total-items="totalBrands"
        :loading="Loading"
        class="elevation-1"
      >
        <template slot="items" slot-scope="props">
          <td class="text-xs-center">{{ props.item.id }}</td>
          <td class="text-xs-center">{{ props.item.name }}</td>
          <td class="text-xs-center"><img  :src="props.item.image" alt=""/></td>
          <td class="text-xs-center">{{ props.item.letter }}</td>
          <td class="text-xs-center">
            <v-btn flat icon color="pink">
              <v-icon>update</v-icon>
            </v-btn>
            <v-btn flat icon color="pink">
              <v-icon>delete</v-icon>
            </v-btn>
          </td>
        </template>
      </v-data-table>
    </div>
</template>

<script>
    import VSpec from "./specification/Specification";
    export default {
        name: "MyBrand",
      components: {VSpec},
      data(){
          return{
            headers:[
              {text: "id",value: "id", align: 'center', sortable: true},
              {text: "名称",value: "name", align: 'center', sortable: false},
              {text: "LOGO",value: "image", align: 'center', sortable: false},
              {text: "首字母",value: "letter", align: 'center', sortable: true},
              {text: "操作", align: 'center', sortable: false}
            ],
            brands:[],
            pagination:{},
            totalBrands:0,
            Loading:false,
            key:"",//搜索条件
          }
        },
       created(){
          this.brands = [
            {id: 2032, name: "OPPO", image: "1.jpg", letter: "O"},
            {id: 2033, name: "飞利浦（PHILIPS）", image: "2.jpg", letter: "F"},
            {id: 2034, name: "华为（HUAWEI）", image: "3.jpg", letter: "H"},
            {id: 2036, name: "酷派（Coolpad）", image: "4.jpg", letter: "K"},
            {id: 2037, name: "魅族（MEIZU）", image: "5.jpg", letter: "M"}
          ];
          this.totalBrands = 15;

          // 去后台查询中
         this.loadBrands();
       },
      watch:{
          key(){
            this.loadBrands();
          },
        pagination:{
          deep:true,
          handler(){
            this.loadBrands();
          }
        }
      },
      methods:{
          loadBrands(){
            this.loading = true;
            this.$http.get("/item/brand/page",{
              params:{
                page:this.pagination.page,  //当前页
                rows:this.pagination.rowsPerPage,  //每页大小
                sortBy:this.pagination.sortBy,  //排序字段
                desc:this.pagination.descending,  //是否降序
                key:this.key  //搜索条件
              }
            }).then(resp => {
                this.brands = resp.data.items;
                this.totalBrands = resp.data.total;
            })
          }
      }
    }
</script>

<style scoped>

</style>
