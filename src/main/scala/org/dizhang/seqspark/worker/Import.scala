package org.dizhang.seqspark.worker

import com.typesafe.config.Config
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.dizhang.seqspark.ds.{StringGenotype, Genotype, Variant}
import org.dizhang.seqspark.util.InputOutput._
import org.dizhang.seqspark.util.Constant._

/**
 * Always read a dense variant from VCF
 */
object Import extends Worker[String, Genotype] {

  implicit val name = new WorkerName("import")

  def apply(input: String)(implicit cnf: Config, sc: SparkContext): Genotype = {
    val raw = sc.textFile(input)
    val build = cnf.getString("import.build")
    val source = cnf.getString("import.type")
    StringGenotype(makeVariants(raw), cnf.getConfig(ConfigPath.`import`))
  }

  /** filter variants based on
    * 1. filter column in vcf
    * 2. only bi-allelic SNPs if biAllelicSNV is true in Conf
    */

  def load(path: String, sc: SparkContext): Genotype = {
    val data: Genotype = sc.objectFile(path).asInstanceOf[Genotype]
    data
  }

  def makeVariants(raw: RDD[String])(implicit cnf: Config): RawVCF = {
    val genoInCnf = cnf.getConfig("genotypeInput")
    val biAllelicSNV = genoInCnf.getString("biAllelicSNV")
    val default = UnPhased.Gt.ref
    val vars = raw filter (l => ! l.startsWith("#")) map (l => Variant.fromString(l, default))
    val s1 =
      if (genoInCnf.hasPath("filterNot")) {
        val filterNot = genoInCnf.getString("filterNot")
        vars filter (v => !v.filter.matches(filterNot))
      } else
        vars
    val s2 =
      if (genoInCnf.hasPath("filter")) {
        val filter = genoInCnf.getString("filter")
        s1 filter (v => v.filter.matches(filter))
      } else
        s1
    val s3 =
      if (biAllelicSNV == "true")
        s2 filter (v => v.ref.matches("[ATCG]") && v.alt.matches("[ATCG]"))
      else
        s2
    /** save is very time-consuming and resource-demanding */
    if (genoInCnf.getBoolean("save"))
      try {
        s3.saveAsObjectFile(saveDir)
      } catch {
        case e: Exception => {println("Import: save failed"); System.exit(1)}
      }
    s3
  }
}
